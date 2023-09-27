package testcontainers.containers

import java.util.{ List => JList }
import java.util.concurrent.{ Callable, TimeUnit }

import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._

import org.rnorth.ducttape.unreliables.Unreliables
import org.slf4j.LoggerFactory
import org.testcontainers.containers.{ GenericContainer, Network }
import org.testcontainers.lifecycle.Startable
import org.testcontainers.shaded.com.google.common.base.Throwables
import org.testcontainers.shaded.org.awaitility.Awaitility.await

import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.Network.Ipam

import testcontainers.containers.Nebula.dockerClient

/**
 * @author
 *   梦境迷离
 * @version 1.0,2023/9/18
 */
object NebulaClusterContainer {
  private val logger = LoggerFactory.getLogger(classOf[NebulaClusterContainer])
}

abstract class NebulaClusterContainer extends Startable {

  import NebulaClusterContainer.logger

  protected def subnetIp: String

  protected def gatewayIp: String = {
    if (subnetIp == null) {
      throw new IllegalStateException("subnetIp cannot be null")
    }
    if (!subnetIp.contains("/")) {
      throw new IllegalStateException("subnetIp is invalid")
    }
    val sub = subnetIp.split("/")(0)
    increaseLastIp(sub, 1)
  }

  protected val nebulaNet: Network =
    Network
      .builder()
      .createNetworkCmdModifier { cmd =>
        cmd
          .withName(Nebula.NetworkName)
          .withIpam(
            new Ipam()
              .withDriver(Nebula.NetworkType)
              .withConfig(new Ipam.Config().withSubnet(subnetIp).withGateway(gatewayIp))
          )
      }
      .build()
  protected val metaIpPortMapping: List[(String, Int)]
  protected val storageIpMapping: List[(String, Int)]
  protected val graphIpMapping: List[(String, Int)]
  protected val consoleIp: String

  protected lazy val metaAddrs: String = generateIpAddrs(metaIpPortMapping)

  protected def generateIpAddrs(ipPortMapping: List[(String, Int)]): String =
    ipPortMapping.map(kv => s"${kv._1}:${kv._2}").mkString(",")

  private lazy val ryukContainerId: String = {
    val containersResponse = Nebula.TestcontainersRyukContainer
    containersResponse.map(_.getId).orNull
  }

  protected def increaseLastIp(ip: String, num: Int): String = {
    if (ip == null) {
      throw new IllegalStateException("IPAddress cannot be null!")
    }
    val ipSplits = ip.split("\\.").toList
    val last     = ipSplits.last.toInt
    ipSplits.updated(ipSplits.size - 1, last + num).mkString(".")
  }

  protected val metads: List[GenericContainer[_]]

  protected val storageds: List[GenericContainer[_]]

  protected val graphds: List[GenericContainer[_]]

  protected val console: NebulaConsoleContainer

  def existsRunningContainer: Boolean

  private def awaitMappedPort[S <: GenericContainer[S]](container: GenericContainer[S], exposedPort: Int): Int = {
    val containerId = await()
      .atMost(Nebula.NebulaPortAtMost)
      .pollInterval(Nebula.PollInterval)
      .pollInSameThread()
      .until(
        new Callable[String] {
          override def call(): String =
            container.getContainerId
        },
        (id: String) => id != null
      )

    if (containerId != null) {
      container.getMappedPort(exposedPort).intValue()
    } else
      throw new IllegalStateException(
        "Mapped port can only be obtained after the container is started, awaitMappedPort failed!"
      )
  }

  final override def start(): Unit = {
    storageds.foreach { sd =>
      sd.start()
      Unreliables.retryUntilTrue(
        Nebula.StartTimeout.getSeconds.toInt,
        TimeUnit.SECONDS,
        () => {
          val g = sd.execInContainer("ps", "-ef").getStdout
          g != null && g.contains(Nebula.StoragedName)
        }
      )
    }

    graphds.foreach { gd =>
      gd.start()
      Unreliables.retryUntilTrue(
        Nebula.StartTimeout.getSeconds.toInt,
        TimeUnit.SECONDS,
        () => {
          val g = gd.execInContainer("ps", "-ef").getStdout
          g != null && g.contains(Nebula.GraphdName)
        }
      )
    }

    console.start()
    Unreliables.retryUntilTrue(
      Nebula.WaitHostRetryTimes,
      () => {
        // we are waiting to try the storage service online
        val g = console.execInContainer(console.showHostsCommand: _*).getStdout
        g != null && g.contains("ONLINE") && !g.contains("OFFLINE")
      }
    )
  }

  /**
   * Copy from ResourceReaper#removeContainer method
   */
  private final def stopIfExistsRyukContainer(): Unit = {
    var running = false
    try {
      val containerInfo = dockerClient.inspectContainerCmd(ryukContainerId).exec
      running = containerInfo.getState != null && true == containerInfo.getState.getRunning
    } catch {
      case e: NotFoundException =>
        logger.trace(s"Was going to stop container but it apparently no longer exists: ${ryukContainerId}")
        return
      case e: Exception =>
        logger.trace(
          s"Error encountered when checking container for shutdown (ID: $ryukContainerId) - it may not have been stopped, or may already be stopped. Root cause: ${Throwables.getRootCause(e).getMessage}"
        )
        return
    }

    if (running) try {
      logger.trace("Stopping container: ${ryukContainerId}")
      dockerClient.killContainerCmd(ryukContainerId).exec
      logger.trace(s"Stopped container: ${Nebula.Ryuk.stripPrefix("/")}")
    } catch {
      case e: Exception =>
        logger.trace(
          s"Error encountered shutting down container (ID: $ryukContainerId) - it may not have been stopped, or may already be stopped. Root cause: ${Throwables.getRootCause(e).getMessage}"
        )
    }

    try dockerClient.inspectContainerCmd(ryukContainerId).exec
    catch {
      case e: Exception =>
        logger.trace(s"Was going to remove container but it apparently no longer exists: $ryukContainerId")
        return
    }

    try {
      logger.trace(s"Removing container: $ryukContainerId")
      dockerClient.removeContainerCmd(ryukContainerId).withRemoveVolumes(true).withForce(true).exec
      logger.debug(s"Removed container and associated volume(s): ${Nebula.Ryuk.stripPrefix("/")}")
    } catch {
      case e: Exception =>
        logger.trace(
          s"Error encountered shutting down container (ID: $ryukContainerId) - it may not have been stopped, or may already be stopped. Root cause: ${Throwables.getRootCause(e).getMessage}"
        )
    }
  }

  final override def stop(): Unit =
    try {
      val res = Future.sequence(allContainers.map(f => Future(f)).map(_.map(_.stop())))
      Await.result(res, Nebula.StopTimeout.seconds)
      stopIfExistsRyukContainer()
      if (nebulaNet.getId != null) {
        Nebula.removeTestcontainersNetwork(nebulaNet.getId)
      }
    } catch {
      case e: Throwable =>
        logger.error("Stopped all containers failed", e)
    }

  final def allContainers: List[GenericContainer[_]] = metads ++ storageds ++ graphds ++ List(console)

  final def graphdUrlList: List[String] =
    graphds.map(gd => s"http://${gd.getHost}:${awaitMappedPort(gd, Nebula.GraphdExposedPort)}")

  final def metadUrlList: List[String] =
    metads.map(md => s"http://${md.getHost}:${awaitMappedPort(md, Nebula.MetadExposedPort)}")

  final def storagedUrlList: List[String] =
    storageds.map(sd => s"http://${sd.getHost}:${awaitMappedPort(sd, Nebula.StoragedExposedPort)}")

  final def graphdPortList: List[Int] = graphds.map(gd => awaitMappedPort(gd, Nebula.GraphdExposedPort))

  final def metadPortList: List[Int] = metads.map(md => awaitMappedPort(md, Nebula.MetadExposedPort))

  final def storagedPortList: List[Int] = storageds.map(sd => awaitMappedPort(sd, Nebula.StoragedExposedPort))

  final def graphdHostList: List[String] = graphds.map(_.getHost)

  final def metadHostList: List[String] = metads.map(_.getHost)

  final def storagedHostList: List[String] = storageds.map(_.getHost)

  final def metadList: List[GenericContainer[_]] = metads

  final def storagedList: List[GenericContainer[_]] = storageds

  final def graphdList: List[GenericContainer[_]] = graphds

  /**
   * *********************Java API**************************
   */
  final def getGraphdPortList: JList[Integer] = graphdPortList.map(Integer.valueOf).asJava

  final def getMetadPortList: JList[Integer] = metadPortList.map(Integer.valueOf).asJava

  final def getStoragedPortList: JList[Integer] = storagedPortList.map(Integer.valueOf).asJava

  final def getMetadList: JList[GenericContainer[_]] = metadList.asJava

  final def getStoragedList: JList[GenericContainer[_]] = storagedList.asJava

  final def getGraphdList: JList[GenericContainer[_]] = graphdList.asJava

  final def getGraphdHostList: JList[String] = graphdHostList.asJava

  final def getMetadHostList: JList[String] = metadHostList.asJava

  final def getStoragedHostList: JList[String] = storagedHostList.asJava

  final def getAllContainers: JList[GenericContainer[_]] = allContainers.asJava

  final def getGraphdUrlList: JList[String] =
    graphdUrlList.asJava

  final def getMetadUrlList: JList[String] =
    metadUrlList.asJava

  final def getStoragedUrlList: JList[String] =
    storagedUrlList.asJava

}

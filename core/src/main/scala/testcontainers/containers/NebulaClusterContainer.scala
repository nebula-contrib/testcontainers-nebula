package testcontainers.containers

import java.util.{ List => JList }
import java.util.concurrent.TimeUnit

import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._

import org.rnorth.ducttape.unreliables.Unreliables
import org.testcontainers.containers.GenericContainer
import org.testcontainers.lifecycle.Startable

/**
 * @author
 *   梦境迷离
 * @version 1.0,2023/9/18
 */
abstract class NebulaClusterContainer extends Startable {

  protected val MetaIpPortMapping: List[(String, Int)]
  protected val StorageIpMapping: List[(String, Int)]
  protected val GraphIpMapping: List[(String, Int)]
  protected val ConsoleIp: String

  protected lazy val metaAddrs: String = generateIpAddrs(MetaIpPortMapping)

  protected def generateIpAddrs(ipPortMapping: List[(String, Int)]): String =
    ipPortMapping.map(kv => s"${kv._1}:${kv._2}").mkString(",")

  private val networkType = "bridge"

  private lazy val testcontainersContainerId: String = Nebula.dockerClient
    .listContainersCmd()
    .exec()
    .asScala
    .toList
    .filter { c =>
      c.getNames.toList.exists(_.startsWith("/testcontainers-ryuk"))
    }
    .map(_.getId)
    .headOption
    .orNull

  private lazy val testcontainersContainerIp: String =
    Nebula.dockerClient
      .inspectContainerCmd(testcontainersContainerId)
      .exec()
      .getNetworkSettings
      .getNetworks
      .asScala
      .get(networkType)
      .map(_.getIpAddress)
      .orNull

  protected def increaseIpBasedOnRyukIp(num: Int): String = {
    if (testcontainersContainerIp == null) {
      throw new IllegalStateException("IPAddress cannot be null!")
    }
    val ipSplits = testcontainersContainerIp.split("\\.").toList
    val last     = ipSplits.last.toInt
    ipSplits.updated(ipSplits.size - 1, last + num).mkString(".")
  }

  protected val metads: List[GenericContainer[_]]

  protected val storageds: List[GenericContainer[_]]

  protected val graphds: List[GenericContainer[_]]

  protected val console: NebulaConsoleContainer

  def existsRunningContainer: Boolean

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

  final override def stop(): Unit = {
    val res = Future.sequence(allContainers.map(f => Future(f)).map(_.map(_.stop())))
    Await.result(res, Nebula.StopTimeout.seconds)
  }

  final def allContainers: List[GenericContainer[_]] = metads ++ storageds ++ graphds ++ List(console)

  final def graphdUrlList: List[String] =
    graphds.map(gd => s"http://${gd.getHost}:${gd.getMappedPort(Nebula.GraphdExposedPort)}")

  final def metadUrlList: List[String] =
    metads.map(md => s"http://${md.getHost}:${md.getMappedPort(Nebula.MetadExposedPort)}")

  final def storagedUrlList: List[String] =
    storageds.map(sd => s"http://${sd.getHost}:${sd.getMappedPort(Nebula.StoragedExposedPort)}")

  final def graphdPortList: List[Int] = graphds.map(_.getMappedPort(Nebula.GraphdExposedPort).intValue())

  final def metadPortList: List[Int] = metads.map(_.getMappedPort(Nebula.MetadExposedPort).intValue())

  final def storagedPortList: List[Int] = storageds.map(_.getMappedPort(Nebula.StoragedExposedPort).intValue())

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

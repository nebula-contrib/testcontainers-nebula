package testcontainers.containers

import java.util.{ List => JList }
import java.util.concurrent.TimeUnit

import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._

import org.rnorth.ducttape.unreliables.Unreliables
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.lifecycle.Startable

/**
 * @author
 *   梦境迷离
 * @version 1.0,2023/9/18
 */
abstract class NebulaClusterContainer extends Startable {

  /**
   * docker container ip
   */
  protected lazy val gateway: String = DockerClientFactory
    .lazyClient()
    .listNetworksCmd()
    .withNameFilter("bridge")
    .exec()
    .asScala
    .headOption
    .flatMap(_.getIpam.getConfig.asScala.map(s => s.getGateway).headOption)
    .head // bridge is a pre-defined network and cannot be removed

  protected def increaseIpLastNum(ip: String, num: Int): String = {
    val ipSplits = ip.split("\\.").toList
    val last     = ipSplits.last.toInt
    ipSplits.updated(ipSplits.size - 1, last + num).mkString(".")
  }

  protected val metad: List[GenericContainer[_]] = List.empty

  protected val storaged: List[GenericContainer[_]] = List.empty

  protected val graphd: List[GenericContainer[_]] = List.empty

  protected val console: NebulaConsoleContainer

  def existsRunningContainer: Boolean

  final override def start(): Unit = {
    storaged.foreach { sd =>
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

    graphd.foreach { gd =>
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

  final def allContainers: List[GenericContainer[_]] = List(metad, storaged, graphd, List(console)).flatten

  final def graphdUrlList: List[String] =
    graphd.map(gd => s"http://${gd.getHost}:${gd.getMappedPort(Nebula.GraphdExposedPort)}")

  final def metadUrlList: List[String] =
    metad.map(md => s"http://${md.getHost}:${md.getMappedPort(Nebula.MetadExposedPort)}")

  final def storagedUrlList: List[String] =
    storaged.map(sd => s"http://${sd.getHost}:${sd.getMappedPort(Nebula.StoragedExposedPort)}")

  final def graphdPortList: List[Int] = graphd.map(_.getMappedPort(Nebula.GraphdExposedPort).intValue())

  final def metadPortList: List[Int] = metad.map(_.getMappedPort(Nebula.MetadExposedPort).intValue())

  final def storagedPortList: List[Int] = storaged.map(_.getMappedPort(Nebula.StoragedExposedPort).intValue())

  final def graphdHostList: List[String] = graphd.map(_.getHost)

  final def metadHostList: List[String] = metad.map(_.getHost)

  final def storagedHostList: List[String] = storaged.map(_.getHost)

  final def metadList: List[GenericContainer[_]] = metad

  final def storagedList: List[GenericContainer[_]] = storaged

  final def graphdList: List[GenericContainer[_]] = graphd

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

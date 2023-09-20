package testcontainers.containers

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
    .head

  protected def increaseIpLastNum(ip: String, num: Int): String = {
    val ipSplits = ip.split("\\.").toList
    val last     = ipSplits.last.toInt
    ipSplits.updated(ipSplits.size - 1, last + num).mkString(".")
  }

  protected val metad: List[GenericContainer[_]] = List.empty

  protected val storaged: List[GenericContainer[_]] = List.empty

  protected val graphd: List[GenericContainer[_]] = List.empty

  protected val console: NebulaConsoleContainer

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

  final def graphdUrl: List[String] =
    graphd.map(gd => String.format("http://" + gd.getHost + ":" + gd.getMappedPort(Nebula.GraphdExposedPort)))

  final def metadUrl: List[String] =
    metad.map(md => String.format("http://" + md.getHost + ":" + md.getMappedPort(Nebula.MetadExposedPort)))

  final def storagedUrl: List[String] =
    storaged.map(sd => String.format("http://" + sd.getHost + ":" + sd.getMappedPort(Nebula.StoragedExposedPort)))

  final def graphdPort: List[Int] = graphd.map(_.getMappedPort(Nebula.GraphdExposedPort))

  final def metadPort: List[Int] = metad.map(_.getMappedPort(Nebula.MetadExposedPort))

  final def storagedPort: List[Int] = storaged.map(_.getMappedPort(Nebula.StoragedExposedPort))

  final def graphdPortJava: List[Integer] = graphdPort.map(Integer.valueOf)

  final def metadPortJava: List[Integer] = metadPort.map(Integer.valueOf)

  final def storagedPortJava: List[Integer] = storagedPort.map(Integer.valueOf)

  final def graphdHost: List[String] = graphd.map(_.getHost)

  final def metadHost: List[String] = metad.map(_.getHost)

  final def storagedHost: List[String] = storaged.map(_.getHost)

  final def getMetad: List[GenericContainer[_]] = metad

  final def getStoraged: List[GenericContainer[_]] = storaged

  final def getGraphd: List[GenericContainer[_]] = graphd

}

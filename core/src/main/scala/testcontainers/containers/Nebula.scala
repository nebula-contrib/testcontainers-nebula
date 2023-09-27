package testcontainers.containers

import java.time.Duration
import java.util.concurrent.Callable

import scala.jdk.CollectionConverters._

import org.testcontainers.DockerClientFactory
import org.testcontainers.shaded.org.awaitility.Awaitility.await
import org.testcontainers.utility.{ DockerImageName, ResourceReaper }

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.Container

/**
 * @author
 *   梦境迷离
 * @version 1.0,2023/9/18
 */
object Nebula {

  /**
   * global constants
   */
  final val DefaultTag         = "v3.6.0"
  final val Username           = "root"
  final val Password           = "nebula"
  final val StartTimeout       = Duration.ofSeconds(30)
  final val WaitHostRetryTimes = 30
  final val StopTimeout        = 30
  final val MinLogLevel        = 0
  final val LOGLevel           = 0
  final val TZ                 = "Asia/Shanghai"
  final val NetworkName        = "testcontainers-nebula-network"
  final val NetworkType        = "default"

  final val PollInterval     = Duration.ofMillis(50)
  final val ContainerAtMost  = Duration.ofSeconds(10)
  final val NebulaPortAtMost = Duration.ofSeconds(5)

  /**
   * default log path
   */
  final val GraphdLogPath   = "/logs/graph"
  final val MetadLogPath    = "/logs/meta"
  final val StoragedLogPath = "/logs/storage"

  /**
   * default data path
   */
  final val DataMetadPath    = "/data/meta"
  final val DataStoragedPath = "/data/storage"

  /**
   * default graph port
   */
  final val GraphdExposedPort = 9669

  /**
   * default meta port
   */
  final val MetadExposedPort = 9559

  /**
   * default storage port
   */
  final val StoragedExposedPort = 9779

  /**
   * default image name
   */
  final val DefaultGraphdImageName   = DockerImageName.parse("vesoft/nebula-graphd")
  final val DefaultMetadImageName    = DockerImageName.parse("vesoft/nebula-metad")
  final val DefaultStoragedImageName = DockerImageName.parse("vesoft/nebula-storaged")
  final val DefaultConsoleImageName  = DockerImageName.parse("vesoft/nebula-console")

  /**
   * docker container name
   */
  final val GraphdName   = "graphd"
  final val MetadName    = "metad"
  final val StoragedName = "storaged"
  final val ConsoleName  = "console"

  final val dockerClient: DockerClient = DockerClientFactory.lazyClient()

  final val Ryuk = "/testcontainers-ryuk"

  final lazy val TestcontainersRyukContainer = {
    val containersResponse = await()
      .atMost(Nebula.ContainerAtMost)
      .pollInterval(Nebula.PollInterval)
      .pollInSameThread()
      .until(
        new Callable[List[Container]] {
          override def call(): List[Container] =
            Nebula.dockerClient
              .listContainersCmd()
              .exec()
              .asScala
              .toList
        },
        (cs: List[Container]) =>
          cs.filter { c =>
            c.getNames.toList.exists(_.startsWith(Ryuk))
          }.flatMap(_.getNames.headOption.toList).headOption.nonEmpty
      )

    containersResponse.headOption
  }

  final lazy val SessionId = {
    val ryukName = TestcontainersRyukContainer.map(_.getNames.headOption.toList).toList.flatten.headOption
    ryukName.map(_.stripPrefix(Ryuk + "-")).getOrElse(DockerClientFactory.SESSION_ID)
  }

  def removeTestcontainersNetwork(networkId: String): Unit =
    Nebula.dockerClient
      .removeNetworkCmd(networkId)
      .exec()

}

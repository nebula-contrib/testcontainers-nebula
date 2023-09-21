package testcontainers.containers

import java.time.Duration

import scala.jdk.CollectionConverters._

import org.testcontainers.DockerClientFactory
import org.testcontainers.utility.DockerImageName

import com.github.dockerjava.api.DockerClient

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

  final lazy val SessionId =
    dockerClient
      .listContainersCmd()
      .exec()
      .asScala
      .toList
      .filter { c =>
        c.getNames.toList.exists(_.startsWith(Ryuk))
      }
      .flatMap(_.getNames.headOption.toList)
      .headOption
      .map(_.stripPrefix(Ryuk + "-"))
      .getOrElse(DockerClientFactory.SESSION_ID)

}

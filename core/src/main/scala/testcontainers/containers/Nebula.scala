package testcontainers.containers

import java.time.Duration

import org.testcontainers.utility.DockerImageName

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
  // Specifies the minimum level of the log. That is, no logs below this level will be printed.
  // Optional values are 0 (INFO), 1 (WARNING), 2 (ERROR), 3 (FATAL).
  // It is recommended to set it to 0 during debugging and 1 in a production environment.
  // If it is set to 4, NebulaGraph will not print any logs.
  final val MinLogLevel        = 0
  // Specifies the detailed level of the log. The larger the value, the more detailed the log is.
  // Optional values are 0, 1, 2, 3.
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

}

package testcontainers.containers

import scala.jdk.OptionConverters._

import org.slf4j.{ Logger, LoggerFactory }
import org.testcontainers.containers.GenericContainer

/**
 * @author
 *   梦境迷离
 * @version 1.0,2023/9/18
 */
object NebulaSimpleClusterContainer {
  private val logger: Logger = LoggerFactory.getLogger(classOf[NebulaClusterContainer])
}

/**
 * The simplest Nebula service, with one storaged, one metad, and one graphd.
 *
 * @param version
 *   The image version/tag
 * @param absoluteHostPathPrefix
 *   The prefix of your host path, eg: prefix/data/meta1:/data/meta, prefix/logs/meta1:/logs
 */
final class NebulaSimpleClusterContainer(
  version: String = Nebula.DefaultTag,
  absoluteHostPathPrefix: Option[String] = None
) extends NebulaClusterContainer {

  import NebulaSimpleClusterContainer._

  def this(version: String, absoluteBindingPath: java.util.Optional[String]) =
    this(version, absoluteBindingPath.toScala)

  private val MetaIp    = increaseIpLastNum(gateway, 2)
  private val StorageIp = increaseIpLastNum(gateway, 3)
  private val GraphIp   = increaseIpLastNum(gateway, 4)
  private val ConsoleIp = increaseIpLastNum(gateway, 5)

  logger.info(s"Nebula metad started at ip: $MetaIp")
  logger.info(s"Nebula storaged started at ip: $StorageIp")
  logger.info(s"Nebula graphd started at ip: $GraphIp")

  protected override val metad: List[GenericContainer[_]] = List(
    new NebulaMetadContainer(
      version,
      MetaIp,
      s"$MetaIp:${Nebula.MetadExposedPort}",
      NebulaMetadContainer.defaultPortBindings(Nebula.MetadExposedPort),
      absoluteHostPathPrefix.fold(List.empty[NebulaVolume])(p => NebulaMetadContainer.defaultVolumeBindings(p))
    )
  )

  protected override val storaged: List[GenericContainer[_]] = List(
    new NebulaStoragedContainer(
      version,
      StorageIp,
      s"$MetaIp:${Nebula.MetadExposedPort}",
      NebulaStoragedContainer.defaultPortBindings(Nebula.StoragedExposedPort),
      absoluteHostPathPrefix.fold(List.empty[NebulaVolume])(p => NebulaStoragedContainer.defaultVolumeBindings(p))
    )
  ).map(_.dependsOn(metad: _*))

  protected override val graphd: List[GenericContainer[_]] = List(
    new NebulaGraphdContainer(
      version,
      GraphIp,
      s"$MetaIp:${Nebula.MetadExposedPort}",
      NebulaGraphdContainer.defaultPortBindings(Nebula.GraphdExposedPort),
      absoluteHostPathPrefix.fold(List.empty[NebulaVolume])(p => NebulaGraphdContainer.defaultVolumeBindings(p))
    )
  ).map(_.dependsOn(metad: _*))

  protected override val console: NebulaConsoleContainer =
    new NebulaConsoleContainer(
      version,
      ConsoleIp,
      graphdIp = GraphIp,
      graphdPort = Nebula.GraphdExposedPort,
      storagedAddrs = List(
        StorageIp -> Nebula.StoragedExposedPort
      )
    )
}

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

  // TODO: determine available IPs?
  protected override val MetaIpPortMapping: List[(String, Int)] = List(
    increaseIpBasedOnRyukIp(1) -> Nebula.MetadExposedPort
  )

  protected override val StorageIpMapping: List[(String, Int)] = List(
    increaseIpBasedOnRyukIp(2) -> Nebula.StoragedExposedPort
  )

  protected override val GraphIpMapping: List[(String, Int)] = List(
    increaseIpBasedOnRyukIp(3) -> Nebula.GraphdExposedPort
  )
  protected override val ConsoleIp: String                   = increaseIpBasedOnRyukIp(4)

  logger.info(s"Nebula meta nodes started at ip: ${generateIpAddrs(MetaIpPortMapping)}")
  logger.info(s"Nebula storage nodes started at ip: ${generateIpAddrs(StorageIpMapping)}")
  logger.info(s"Nebula graph nodes started at ip: ${generateIpAddrs(GraphIpMapping)}")

  protected override val metads: List[GenericContainer[_]] =
    MetaIpPortMapping.map { case (ip, port) =>
      new NebulaMetadContainer(
        version,
        ip,
        metaAddrs,
        NebulaMetadContainer.defaultPortBindings(port),
        absoluteHostPathPrefix.fold(List.empty[NebulaVolume])(p => NebulaMetadContainer.defaultVolumeBindings(p))
      )
    }

  protected override val storageds: List[GenericContainer[_]] =
    StorageIpMapping.map { case (ip, port) =>
      new NebulaStoragedContainer(
        version,
        ip,
        metaAddrs,
        NebulaStoragedContainer.defaultPortBindings(port),
        absoluteHostPathPrefix.fold(List.empty[NebulaVolume])(p => NebulaStoragedContainer.defaultVolumeBindings(p))
      )
    }.map(_.dependsOn(metads: _*))

  protected override val graphds: List[GenericContainer[_]] = GraphIpMapping.map { case (ip, port) =>
    new NebulaGraphdContainer(
      version,
      ip,
      metaAddrs,
      NebulaGraphdContainer.defaultPortBindings(port),
      absoluteHostPathPrefix.fold(List.empty[NebulaVolume])(p => NebulaGraphdContainer.defaultVolumeBindings(p))
    )
  }.map(_.dependsOn(metads: _*))

  protected override val console: NebulaConsoleContainer = new NebulaConsoleContainer(
    version,
    ConsoleIp,
    graphdIp = GraphIpMapping.head._1,
    graphdPort = GraphIpMapping.head._2,
    storagedAddrs = StorageIpMapping
  )

  override def existsRunningContainer: Boolean =
    metads.exists(_.isRunning) || storageds.exists(_.isRunning) || graphds.exists(_.isRunning) || console.isRunning
}

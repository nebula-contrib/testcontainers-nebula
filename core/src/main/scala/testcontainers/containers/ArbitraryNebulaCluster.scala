package testcontainers.containers

import scala.jdk.OptionConverters._

/**
 * The simplest Nebula service, with one storaged, one metad, and one graphd.
 * @param cluster
 *   The number of graphd/metad/storaged in the cluster
 * @param version
 *   The image version/tag
 * @param absoluteHostPathPrefix
 *   The prefix of your host path, eg: prefix/data/meta1:/data/meta, prefix/logs/meta1:/logs
 */
final case class ArbitraryNebulaCluster(
  cluster: Int = 1,
  version: String = Nebula.DefaultTag,
  absoluteHostPathPrefix: Option[String] = None,
  subnetIp: String = "172.28.0.0/16"
) extends NebulaClusterContainer(cluster, version, absoluteHostPathPrefix, subnetIp) {

  def this(version: String) =
    this(1, version, java.util.Optional.empty().toScala)

  def this(cluster: Int) =
    this(cluster, Nebula.DefaultTag, java.util.Optional.empty().toScala)

  def this(cluster: Int, version: String, absoluteBindingPath: java.util.Optional[String]) =
    this(cluster, version, absoluteBindingPath.toScala)
}

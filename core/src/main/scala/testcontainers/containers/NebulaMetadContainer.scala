package testcontainers.containers

import org.testcontainers.containers.BindMode
import org.testcontainers.utility.DockerImageName

import com.github.dockerjava.api.model._

import testcontainers.containers.Nebula._

/**
 * @author
 *   梦境迷离
 * @version 1.0,2023/9/18
 */
object NebulaMetadContainer {

  def defaultPortBindings(bindingPort: Int): List[PortBinding] =
    List(
      new PortBinding(
        Ports.Binding.bindPort(bindingPort),
        new ExposedPort(Nebula.MetadExposedPort) // docker container port
      )
    )

  def defaultVolumeBindings(absoluteHostPathPrefix: String, instanceIndex: Int): List[NebulaVolume] =
    List(
      NebulaVolume(
        absoluteHostPathPrefix + Nebula.MetadLogPath + instanceIndex,
        Nebula.MetadLogPath,
        BindMode.READ_WRITE
      ),
      NebulaVolume(
        absoluteHostPathPrefix + Nebula.DataMetadPath + instanceIndex,
        Nebula.DataMetadPath,
        BindMode.READ_WRITE
      )
    )
}

final class NebulaMetadContainer(
  dockerImageName: DockerImageName,
  val containerIp: String,
  val metaAddrs: String,
  val portsBindings: List[PortBinding],
  val bindings: List[NebulaVolume],
  instanceIndex: Int
) extends BaseContainer[NebulaMetadContainer](dockerImageName) {

  def this(
    version: String,
    containerIp: String,
    metaAddrs: String,
    portsBindings: List[PortBinding],
    bindings: List[NebulaVolume],
    instanceIndex: Int
  ) =
    this(Nebula.DefaultMetadImageName.withTag(version), containerIp, metaAddrs, portsBindings, bindings, instanceIndex)

  override def getContainerName: String = Nebula.MetadName + instanceIndex + "-" + Nebula.SessionId

  override def commands(containerIp: String, metaAddrs: String): Seq[String] =
    Seq(
      s"--meta_server_addrs=$metaAddrs",
      s"--local_ip=$containerIp",
      s"--port=${portsBindings.headOption.flatMap(_.getBinding.getHostPortSpec.split(":").headOption).getOrElse(Nebula.MetadExposedPort)}",
      s"--log_dir=$MetadLogPath",
      s"--v=$LOGLevel",
      s"--minloglevel=$MinLogLevel"
    )
}

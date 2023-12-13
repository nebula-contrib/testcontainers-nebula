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
object NebulaStoragedContainer {

  def defaultPortBindings(bindingPort: Int): List[PortBinding] =
    List(
      new PortBinding(
        Ports.Binding.bindPort(bindingPort),
        new ExposedPort(Nebula.StoragedExposedPort) // docker container port
      )
    )

  def defaultVolumeBindings(absoluteHostPathPrefix: String, instanceIndex: Int): List[NebulaVolume] =
    List(
      NebulaVolume(
        absoluteHostPathPrefix + Nebula.DataStoragedPath + instanceIndex,
        Nebula.DataStoragedPath,
        BindMode.READ_WRITE
      ),
      NebulaVolume(
        absoluteHostPathPrefix + Nebula.StoragedLogPath + instanceIndex,
        Nebula.StoragedLogPath,
        BindMode.READ_WRITE
      )
    )
}

final class NebulaStoragedContainer(
  dockerImageName: DockerImageName,
  val containerIp: String,
  val metaAddrs: String,
  val portsBindings: List[PortBinding],
  val bindings: List[NebulaVolume],
  instanceIndex: Int
) extends BaseContainer[NebulaStoragedContainer](dockerImageName) {

  def this(
    version: String,
    containerIp: String,
    metaAddrs: String,
    portsBindings: List[PortBinding],
    bindings: List[NebulaVolume],
    instanceIndex: Int
  ) =
    this(
      Nebula.DefaultStoragedImageName.withTag(version),
      containerIp,
      metaAddrs,
      portsBindings,
      bindings,
      instanceIndex
    )

  override def getContainerName: String = Nebula.StoragedName + instanceIndex + "-" + Nebula.SessionId

  override def commands(containerIp: String, metaAddrs: String): Seq[String] =
    Seq(
      s"--meta_server_addrs=$metaAddrs",
      s"--local_ip=$containerIp",
      s"--port=${portsBindings.headOption.flatMap(_.getBinding.getHostPortSpec.split(":").headOption).getOrElse(Nebula.StoragedExposedPort)}",
      s"--log_dir=$StoragedLogPath",
      s"--v=$LOGLevel",
      s"--minloglevel=$MinLogLevel"
    )
}

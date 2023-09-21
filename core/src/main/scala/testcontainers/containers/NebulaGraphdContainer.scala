package testcontainers
package containers

import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.BindMode
import org.testcontainers.utility.DockerImageName

import com.github.dockerjava.api.model._

import testcontainers.containers.Nebula._

/**
 * @author
 *   梦境迷离
 * @version 1.0,2023/9/18
 */
object NebulaGraphdContainer {

  def defaultPortBindings(bindingPort: Int): List[PortBinding] =
    List(
      new PortBinding(
        Ports.Binding.bindPort(bindingPort),
        new ExposedPort(Nebula.GraphdExposedPort) // docker container port
      )
    )

  def defaultVolumeBindings(absoluteHostPathPrefix: String, instanceIndex: Int = 1): List[NebulaVolume] =
    List(
      NebulaVolume(
        absoluteHostPathPrefix + Nebula.GraphdLogPath + instanceIndex,
        Nebula.GraphdLogPath,
        BindMode.READ_WRITE
      )
    )
}

final class NebulaGraphdContainer(
  dockerImageName: DockerImageName,
  val containerIp: String,
  val metaAddrs: String,
  val portsBindings: List[PortBinding],
  val bindings: List[NebulaVolume],
  instanceIndex: Int
) extends BaseContainer[NebulaGraphdContainer](dockerImageName) {

  def this(
    version: String,
    containerIp: String,
    metaAddrs: String,
    portsBindings: List[PortBinding],
    bindings: List[NebulaVolume],
    instanceIndex: Int = 1
  ) =
    this(Nebula.DefaultGraphdImageName.withTag(version), containerIp, metaAddrs, portsBindings, bindings, instanceIndex)

  override def getContainerName: String = Nebula.GraphdName + instanceIndex + "-" + Nebula.SessionId

  override def commands(containerIp: String, metaAddrs: String): Seq[String] =
    Seq(
      s"--meta_server_addrs=$metaAddrs",
      s"--port=$GraphdExposedPort",
      s"--local_ip=$containerIp",
      s"--log_dir=$GraphdLogPath",
      s"--v=$LOGLevel",
      s"--minloglevel=$MinLogLevel"
    )
}

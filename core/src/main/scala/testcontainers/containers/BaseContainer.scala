package testcontainers.containers

import java.util

import scala.jdk.CollectionConverters._

import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

import com.github.dockerjava.api.model._

/**
 * @author
 *   梦境迷离
 * @version 1.0,2023/9/19
 */
abstract class BaseContainer[T <: GenericContainer[T]](dockerImageName: DockerImageName)
    extends GenericContainer[T](dockerImageName) {

  val containerIp: String
  val metaAddrs: String
  val portsBindings: List[PortBinding]
  val bindings: List[NebulaVolume]

  def commands(containerIp: String, metaAddrs: String): Seq[String]

  override def getLivenessCheckPortNumbers: util.Set[Integer] =
    portsBindings.map(_.getExposedPort.getPort).toSet.map(this.getMappedPort).asJava

  def customHostConfig: HostConfig =
    new HostConfig().withPortBindings(portsBindings: _*)

  this
    .withEnv("USER", Nebula.Username)
    .withEnv("TZ", Nebula.TZ)
    .withStartupTimeout(Nebula.StartTimeout)
    .withCreateContainerCmdModifier(cmd =>
      cmd
        .withName(getContainerName)
        .withHostConfig(customHostConfig)
        .withIpv4Address(containerIp)
    )
    .withCommand(commands(containerIp, metaAddrs): _*)

  bindings.foreach(volume => this.withFileSystemBind(volume.hostPath, volume.containerPath, volume.mode))
}

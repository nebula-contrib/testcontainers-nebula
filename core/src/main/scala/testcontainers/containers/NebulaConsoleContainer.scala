package testcontainers.containers

import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/**
 * Connect to console: nebula-console -addr graphdIp -port graphdPort -u root -p nebula
 *
 * @author
 *   梦境迷离
 * @version 1.0,2023/9/18
 */
final class NebulaConsoleContainer(
  dockerImageName: DockerImageName,
  graphdIp: String,
  graphdPort: Int,
  storagedAddrs: List[(String, Int)]
) extends GenericContainer[NebulaConsoleContainer](dockerImageName) {

  def this(
    version: String,
    graphdIp: String,
    graphdPort: Int,
    storagedAddrs: List[(String, Int)]
  ) =
    this(
      Nebula.DefaultConsoleImageName.withTag(version),
      graphdIp,
      graphdPort,
      storagedAddrs
    )

  override def getContainerName: String = Nebula.ConsoleName + "-" + Nebula.SessionId

  def showHostsCommand: Seq[String] = Seq(
    s"sh",
    s"-c",
    s"""sleep 3 && nebula-console -addr $graphdIp -port $graphdPort -u ${Nebula.Username} -p ${Nebula.Password} -e 'SHOW HOSTS' && exit"""
  )

  def commands: Seq[String] = {
    val adds = storagedAddrs.map(a => s""""${a._1}":${a._2}""").mkString(",")
    Seq(
      s"sh",
      s"-c",
      s"""sleep 3 && nebula-console -addr $graphdIp -port $graphdPort -u ${Nebula.Username} -p ${Nebula.Password} -e 'ADD HOSTS $adds' && sleep 36000"""
    )
  }

  this
    .withStartupTimeout(Nebula.StartTimeout)
    .withCreateContainerCmdModifier(cmd =>
      cmd
        .withName(getContainerName)
        .withEntrypoint("")
        .getHostConfig
        .withAutoRemove(true)
    )
    .withCommand(commands: _*)

}

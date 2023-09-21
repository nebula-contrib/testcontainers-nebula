package testcontainers.containers.znebula

import zio.{ URIO, ZIO }

import testcontainers.containers.NebulaClusterContainer

/**
 * @author
 *   梦境迷离
 * @version 1.0,2023/9/21
 */
object ext {

  implicit final class NebulaContainerExtension(val container: NebulaClusterContainer) extends AnyVal {
    def stopAsync: URIO[Any, Unit] = ZIO.attempt(container.stop()).ignoreLogged
  }

}

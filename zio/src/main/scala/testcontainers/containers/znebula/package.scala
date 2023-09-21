package testcontainers.containers

import zio._

/**
 * @author
 *   梦境迷离
 * @version 1.0,2023/9/21
 */
package object znebula {

  implicit final class ContainerExtension(val container: NebulaClusterContainer) extends AnyVal {
    def stopAsync: URIO[Any, Unit] = ZIO.attempt(container.stop()).ignoreLogged
  }
}

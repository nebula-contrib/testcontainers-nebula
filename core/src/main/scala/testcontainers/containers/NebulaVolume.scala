package testcontainers.containers

import org.testcontainers.containers.BindMode

/**
 * @author
 *   梦境迷离
 * @version 1.0,2023/9/20
 */
final case class NebulaVolume(
  hostPath: String,
  containerPath: String,
  mode: BindMode
)

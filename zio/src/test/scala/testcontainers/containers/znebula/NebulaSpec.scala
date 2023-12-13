package testcontainers.containers.znebula

import zio._
import zio.nebula._
import zio.nebula.net.{ NebulaClient, Stmt }
import zio.test._
import zio.test.TestAspect._

import testcontainers.containers.ArbitraryNebulaCluster
import testcontainers.containers.znebula.ext._

trait NebulaSpec extends ZIOSpecDefault {

  type Nebula = Client with Storage with Meta with Scope

  zio.nebula.GlobalSettings.printStatement = true
  zio.nebula.GlobalSettings.logLevel = LogLevel.Warning

  override def aspects: Chunk[TestAspectAtLeastR[TestEnvironment]] =
    Chunk(TestAspect.fibers, TestAspect.timeout(180.seconds))

  val container: ArbitraryNebulaCluster = new ArbitraryNebulaCluster
  container.start()

  println(container.metadUrlList)
  println(container.graphdUrlList)

  val meta    = NebulaTestEnvironment.defaultMeta(container.metadHostList.head, container.metadPortList.head)
  val client  = NebulaTestEnvironment.defaultClient(container.graphdHostList.head, container.graphdPortList.head)
  val storage = NebulaTestEnvironment.defaultStorage(container.metadHostList.head, container.metadPortList.head)

  override def spec =
    (specLayered @@ beforeAll(
      ZIO.serviceWithZIO[NebulaClient](_.init())
        *> ZIO.serviceWithZIO[NebulaClient](
          _.openSession().flatMap(
            _.execute(
              Stmt.str(s"""
                          |CREATE SPACE IF NOT EXISTS ${NebulaTestEnvironment.defaultSpace}(vid_type=fixed_string(20));
                          |USE ${NebulaTestEnvironment.defaultSpace};
                          |CREATE TAG IF NOT EXISTS person(name string, age int);
                          |CREATE EDGE IF NOT EXISTS like(likeness double)
                          |""".stripMargin)
            )
          )
        )
    ) @@ sequential @@ eventually @@ afterAll(container.stopAsync))
      .provideShared(
        Scope.default,
        meta,
        client,
        storage
      )

  def specLayered: Spec[Nebula, Throwable]
}

package testcontainers.containers.zio

import zio._
import zio.nebula._
import zio.nebula.net.{ NebulaClient, Stmt }
import zio.test._
import zio.test.TestAspect._

import testcontainers.containers.NebulaSimpleClusterContainer

trait NebulaSpec extends ZIOSpecDefault {

  type Nebula = Client with SessionClient with Storage with Meta with Scope

  zio.nebula.GlobalSettings.printStatement = true
  zio.nebula.GlobalSettings.logLevel = LogLevel.Warning

  override def aspects: Chunk[TestAspectAtLeastR[TestEnvironment]] =
    Chunk(TestAspect.fibers, TestAspect.timeout(180.seconds))

  val container: NebulaSimpleClusterContainer = new NebulaSimpleClusterContainer
  container.start()

  println(container.metadUrlList)
  println(container.graphdUrlList)

  val meta          = ZioNebulaEnvironment.defaultMeta(container.metadHostList.head, container.metadPortList.head)
  val client        = ZioNebulaEnvironment.defaultClient(container.graphdHostList.head, container.graphdPortList.head)
  val sessionClient = ZioNebulaEnvironment.defaultSession(container.graphdHostList.head, container.graphdPortList.head)
  val storage       = ZioNebulaEnvironment.defaultStorage(container.metadHostList.head, container.metadPortList.head)

  override def spec =
    (specLayered @@ beforeAll(
      ZIO.serviceWithZIO[NebulaClient](_.init())
        *> ZIO.serviceWithZIO[NebulaClient](
          _.openSession().flatMap(
            _.execute(
              Stmt.str(s"""
                          |CREATE SPACE IF NOT EXISTS ${ZioNebulaEnvironment.defaultSpace}(vid_type=fixed_string(20));
                          |USE ${ZioNebulaEnvironment.defaultSpace};
                          |CREATE TAG IF NOT EXISTS person(name string, age int);
                          |CREATE EDGE IF NOT EXISTS like(likeness double)
                          |""".stripMargin)
            )
          )
        )
    ) @@ sequential @@ eventually @@ afterAll(ZIO.attempt(container.start()).ignoreLogged))
      .provideShared(
        Scope.default,
        meta,
        client,
        sessionClient,
        storage
      )

  def specLayered: Spec[Nebula, Throwable]
}

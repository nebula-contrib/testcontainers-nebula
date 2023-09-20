package testcontainers.containers.zio

import zio.ZIO
import zio.nebula._
import zio.nebula.meta._
import zio.nebula.storage._
import zio.test._

/**
 * @author
 *   梦境迷离
 * @version 1.0,2023/8/28
 */
object NebulaClientSpec extends NebulaSpec {

  val insertVertexes =
    """
      |INSERT VERTEX person(name, age) VALUES 
      |'Bob':('Bob', 10), 
      |'Lily':('Lily', 9),'Tom':('Tom', 10),
      |'Jerry':('Jerry', 13),
      |'John':('John', 11);""".stripMargin

  val insertEdges =
    """
      |INSERT EDGE like(likeness) VALUES
      |'Bob'->'Lily':(80.0),
      |'Bob'->'Tom':(70.0),
      |'Lily'->'Jerry':(84.0),
      |'Tom'->'Jerry':(68.3),
      |'Bob'->'John':(97.2);""".stripMargin

  val query =
    """
      |MATCH (p:person) RETURN p LIMIT 4;
      |""".stripMargin

  def specLayered: Spec[Nebula, Throwable] =
    suite("nebula suite")(
      suite("nebula session pool")(
        test("create and query") {
          for {
            init <- ZIO.serviceWithZIO[NebulaSessionClient](_.init())
            _    <- ZIO.logInfo(s"init session: $init")
            res1 <- ZIO.serviceWithZIO[NebulaSessionClient](_.execute(insertVertexes))
            _    <- ZIO.logInfo(s"exec insert vertex: ${res1.errorMessage}")
            res2 <- ZIO.serviceWithZIO[NebulaSessionClient](_.execute(insertEdges))
            _    <- ZIO.logInfo(s"exec insert edge: ${res2.errorMessage}")
            res3 <- ZIO.serviceWithZIO[NebulaSessionClient](_.execute(query))
            _    <- ZIO.logInfo(s"exec query ${res3.errorMessage}")
          } yield assertTrue(res3.rows.size == 4)
        }
      ),
      suite("nebula meta manager")(
        test("query") {
          for {
            spaceItem <- ZIO.serviceWithZIO[NebulaMetaClient](_.space(ZioNebulaEnvironment.defaultSpace))
            _         <- ZIO.logInfo(s"get space: ${spaceItem.toString}")
            spaceId   <- ZIO.serviceWithZIO[NebulaMetaClient](_.spaceId(ZioNebulaEnvironment.defaultSpace))
            _         <- ZIO.logInfo(s"get space id: ${spaceId.toString}")
          } yield assertTrue(spaceItem != null && spaceId > 0)
        }
      ),
      suite("nebula storage client")(
        test("query") {
          for {
            status     <- ZIO.serviceWithZIO[NebulaStorageClient](_.connect())
            _          <- ZIO.logInfo(s"connect status: ${status.toString}")
            scanResult <- ZIO.serviceWithZIO[NebulaStorageClient](
                            _.scan(ScanEdge(ZioNebulaEnvironment.defaultSpace, None, "like", None))
                          )
            _          <- ZIO.logInfo(s"scan result: $scanResult")
          } yield assertTrue(scanResult.hasNext)
        }
      )
    )

}
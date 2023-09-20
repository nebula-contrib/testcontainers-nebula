val zioVersion       = "2.0.13"
val scala3_Version   = "3.3.1"
val scala2_13Version = "2.13.12"
val scala2_12Version = "2.12.18"

val supportCrossVersionList = Seq(scala3_Version, scala2_13Version, scala2_12Version)

inThisBuild(
  List(
    scalaVersion     := supportCrossVersionList.head,
    homepage         := Some(url("https://github.com/hjfruit/testcontainers-nebula")),
    licenses         := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
    organization     := "io.github.jxnu-liguobin",
    organizationName := "梦境迷离",
    developers       := List(
      Developer(
        id = "jxnu-liguobin",
        name = "梦境迷离",
        email = "dreamylost@outlook.com",
        url = url("https://github.com/jxnu-liguobin")
      )
    )
  )
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

val _zioTests = Seq(
  "dev.zio" %% "zio-test-magnolia" % zioVersion,
  "dev.zio" %% "zio-test"          % zioVersion,
  "dev.zio" %% "zio-test-sbt"      % zioVersion
)

lazy val core = project
  .in(file("core"))
  .settings(
    name                     := "testcontainers-nebula",
    crossScalaVersions       := supportCrossVersionList,
    libraryDependencies ++= Seq(
      "org.testcontainers"      % "testcontainers"          % "1.19.0",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.11.0",
      "ch.qos.logback"          % "logback-classic"         % "1.4.2"  % Test,
      "com.vesoft"              % "client"                  % "3.6.0"  % Test,
      "org.scalatest"          %% "scalatest"               % "3.2.17" % Test
    ),
    Test / parallelExecution := false
  )

lazy val zio = project
  .in(file("zio"))
  .settings(
    name                     := "testcontainers-nebula-zio",
    crossScalaVersions       := supportCrossVersionList,
    libraryDependencies ++= Seq(
      "io.github.jxnu-liguobin" %% "zio-nebula" % "0.1.0" % Provided
    ) ++ _zioTests.map(_ % Test),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    Test / parallelExecution := false
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val examples = project
  .in(file("examples"))
  .settings(
    publish / skip           := true,
    crossScalaVersions       := Nil,
    scalaVersion             := scala2_13Version,
    libraryDependencies ++= Seq(
      "com.vesoft" % "client" % "3.6.0"
    ),
    Test / parallelExecution := false
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val root = project
  .in(file("."))
  .settings(
    crossScalaVersions := Nil,
    publish / skip     := true
  )
  .aggregate(
    core,
    zio,
    examples
  )

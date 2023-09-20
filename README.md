Testcontainers for NebulaGraph
---

![CI][Badge-CI] [![Nexus (Snapshots)][Badge-Snapshots]][Link-Snapshots] [![Sonatype Nexus (Releases)][Badge-Release]][Link-Release]


[Badge-CI]: https://github.com/hjfruit/testcontainers-nebula/actions/workflows/scala.yml/badge.svg
[Badge-Snapshots]: https://img.shields.io/nexus/s/io.github.hjfruit/testcontainers-nebula_3?server=https%3A%2F%2Foss.sonatype.org
[Link-Snapshots]: https://oss.sonatype.org/content/repositories/snapshots/io/github/hjfruit/testcontainers-nebula_3/
[Link-Release]: https://index.scala-lang.org/hjfruit/testcontainers-nebula/testcontainers-nebula
[Badge-Release]: https://index.scala-lang.org/hjfruit/testcontainers-nebula/testcontainers-nebula/latest-by-scala-version.svg?platform=jvm


[Testcontainers](https://github.com/testcontainers/testcontainers-java)  is a Java library that supports JUnit tests, providing lightweight, throwaway instances of common databases, Selenium web browsers, or anything else that can run in a Docker container.

[NebulaGraph](https://github.com/vesoft-inc/nebula) is a popular open-source graph database that can handle large volumes of data with milliseconds of latency, scale up quickly, and have the ability to perform fast graph analytics. NebulaGraph has been widely used for social media, recommendation systems, knowledge graphs, security, capital flows, AI, etc.


## Dependency

Support Scala 3, Scala 2.13 and Scala 2.12:
```scala
libraryDependencies += "io.github.jxnu-liguobin" %% "testcontainers-nebula" % <latest version>
```

## Usage Instructions

These containers were starting:
- graphd
- metad
- storaged
- console
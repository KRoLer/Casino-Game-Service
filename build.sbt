import com.typesafe.sbt.packager.docker._
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

name := "Game-webservice"

version := "0.1"

scalaVersion := "2.12.5"

dockerBaseImage := "openjdk:8-jre-alpine"

dockerCommands ++= Seq(
  Cmd("USER", "root"),
  Cmd("RUN", "apk add --update bash && rm -rf /var/cache/apk/*"),
)

mainClass in Compile := Some("com.casino.GameService")

val akka = "2.5.11"
val akkaHttp = "10.1.0"
val cassandra = "0.83"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-cluster" % akka,
  "com.typesafe.akka" %% "akka-cluster-tools" % akka,
  "com.typesafe.akka" %% "akka-stream" % akka,

  "com.typesafe.akka" %% "akka-http" % akkaHttp,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttp,

  "com.typesafe.akka" %% "akka-persistence" % akka,
  "com.typesafe.akka" %% "akka-persistence-cassandra" % cassandra,
  "com.typesafe.akka" %% "akka-persistence-cassandra-launcher" % cassandra % Test
)
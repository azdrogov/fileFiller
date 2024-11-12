name := "fileFiller"

version := "0.1"

scalaVersion := "2.13.15"

Compile / mainClass := Some("ServerMain")

val http4sVersion = "0.23.29"
val http4sBlaze = "0.23.17"
libraryDependencies ++= Seq(
  "org.apache.poi" % "poi" % "5.3.0",
  "org.apache.poi" % "poi-ooxml" % "5.3.0",
  "org.apache.poi" % "poi-ooxml-lite" % "5.3.0",
  "org.odftoolkit" % "simple-odf" % "0.9.0",
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sBlaze,
  "org.http4s" %% "http4s-blaze-client" % http4sBlaze,
  "com.softwaremill.sttp.tapir" %% "tapir-core" % "1.11.8",
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % "1.11.8",
  "com.softwaremill.sttp.tapir" %% "tapir-files" % "1.11.8",
  "co.fs2" %% "fs2-core" % "3.11.0",
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % "1.11.8"
)
name := "infra.ThrottlingService"

version := "0.1"

scalaVersion := "2.13.2"

val fs2V = "2.3.0"
val catsV = "2.1.3"
val testV = ""

libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-io" % fs2V,
  "org.typelevel" %% "cats-effect" % catsV,
  "org.scalatest" %% "scalatest" % "3.2.0-M4" % Test,
  "org.mockito" %% "mockito-scala" % "1.13.9" % Test,
  "org.scalatest" %% "scalatest-flatspec" % "3.2.0-M4" % Test
)

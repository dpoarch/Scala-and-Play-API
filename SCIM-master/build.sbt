name := """SCIM"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  javaJdbc,
  "com.typesafe.play" %% "anorm" % "2.5.0",
  "com.typesafe.play" % "play-json_2.11" % "2.4.2",
  "mysql" % "mysql-connector-java" % "5.1.23",
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

resolvers += Resolver.url("Typesafe Ivy releases", url("https://repo.typesafe.com/typesafe/ivy-releases"))(Resolver.ivyStylePatterns)


fork in run := false
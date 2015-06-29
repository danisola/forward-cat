import com.typesafe.sbt.less.Import.LessKeys
import com.typesafe.sbt.web.SbtWeb
import play.PlayJava
import com.typesafe.sbt.web.SbtWeb.autoImport._

name := "forward-cat-web"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6"

lazy val root = (project in file(".")).enablePlugins(PlayJava, SbtWeb)

libraryDependencies ++= Seq(
    javaEbean,
    "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
    "redis.clients" % "jedis" % "2.1.0",
    "org.apache.james" % "apache-mailet" % "2.4",
    "com.google.inject" % "guice" % "3.0",
    "javax.mail" % "mail" % "1.4.1",
    "org.quartz-scheduler" % "quartz" % "2.1.7",
    "com.forwardcat" % "common" % "1.0-SNAPSHOT",
    "org.hamcrest" % "hamcrest-all" % "1.3" % "test",
    "org.mockito" % "mockito-all" % "1.9.5" % "test"
)

resolvers += ("Local Maven Repository" at "file:///"+Path.userHome.absolutePath+"/.m2/repository")

pipelineStages := Seq(rjs)

includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"

LessKeys.compress := true
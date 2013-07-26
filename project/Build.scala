import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "forward-cat-server"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    javaCore,
    "redis.clients" % "jedis" % "2.1.0",
    "org.apache.james" % "apache-mailet" % "2.4",
    "com.google.inject" % "guice" % "3.0",
    "javax.mail" % "mail" % "1.4.1",
    "com.typesafe" %% "play-plugins-mailer" % "2.1.0",
    "com.beust" % "jcommander" % "1.30",
    "org.hamcrest" % "hamcrest-all" % "1.3",
    "org.mockito" % "mockito-all" % "1.9.5",
    "org.quartz-scheduler" % "quartz" % "2.1.7",
    "com.forwardcat" % "common" % "1.0-SNAPSHOT",
    "com.typesafe.play.extras" %% "iteratees-extras" % "1.0.1"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += "org.sedis" at "http://pk11-scratch.googlecode.com/svn/trunk",
    resolvers += (
      "Local Maven Repository" at "file:///"+Path.userHome.absolutePath+"/.m2/repository"
      )
  )
}

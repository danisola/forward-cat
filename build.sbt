import play.Project._

name := "forward-cat-web"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
    filters,
    "redis.clients" % "jedis" % "2.1.0",
    "org.apache.james" % "apache-mailet" % "2.4",
    "com.google.inject" % "guice" % "3.0",
    "javax.mail" % "mail" % "1.4.1",
    "com.beust" % "jcommander" % "1.30",
    "org.hamcrest" % "hamcrest-all" % "1.3",
    "org.mockito" % "mockito-all" % "1.9.5",
    "org.quartz-scheduler" % "quartz" % "2.1.7",
    "com.forwardcat" % "common" % "1.0-SNAPSHOT"
)

playJavaSettings
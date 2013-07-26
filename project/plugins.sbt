// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Repository for Sedis
resolvers += "org.sedis" at "http://pk11-scratch.googlecode.com/svn/trunk"

// Sedis
//addSbtPlugin("org.sedis" % "sedis_2.10.0" % "1.1.1")

// Use the Play sbt plugin for Play projects
addSbtPlugin("play" % "sbt-plugin" % "2.1.2")
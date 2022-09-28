// resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
val scalajsBundlerVersion = "0.20.0"
// addDependencyTreePlugin
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.10.0")
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % scalajsBundlerVersion)
addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % scalajsBundlerVersion)
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.3")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.2.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "1.2.0")
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.9.0")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")
addSbtPlugin("com.lightbend.paradox" % "sbt-paradox" % "0.10.2")
addSbtPlugin("io.github.jonas" % "sbt-paradox-material-theme" % "0.6.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.4.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.2")
addSbtPlugin("com.github.sbt" % "sbt-proguard" % "0.5.0")
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.3")
libraryDependencies += "software.amazon.awssdk" % "ecr" % "2.17.281"

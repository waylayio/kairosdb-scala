addSbtPlugin("com.github.sbt" % "sbt-site"      % "1.7.0")
addSbtPlugin("com.github.sbt" % "sbt-release"   % "1.4.0")
addSbtPlugin("com.github.sbt" % "sbt-ghpages"   % "0.8.0")
addSbtPlugin("org.scoverage"  % "sbt-scoverage" % "2.1.0")
addSbtPlugin("org.scoverage"  % "sbt-coveralls" % "1.3.13")
addSbtPlugin("com.github.sbt" % "sbt-pgp"       % "2.2.1")

// scala-xml issues
// TODO remove when everything has migrated to scala-xml 2.x
// https://github.com/scala/bug/issues/12632
ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)

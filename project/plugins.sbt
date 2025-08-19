addSbtPlugin("com.github.sbt" % "sbt-site"       % "1.7.0")
addSbtPlugin("com.github.sbt" % "sbt-release"    % "1.4.0")
addSbtPlugin("com.github.sbt" % "sbt-ghpages"    % "0.8.0")
addSbtPlugin("org.scoverage"  % "sbt-scoverage"  % "2.3.1")
addSbtPlugin("org.scoverage"  % "sbt-coveralls"  % "1.3.15")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.11.2")

// scala-xml issues
// TODO remove when everything has migrated to scala-xml 2.x
// https://github.com/scala/bug/issues/12632
ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)

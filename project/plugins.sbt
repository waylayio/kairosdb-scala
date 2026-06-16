//addSbtPlugin("com.github.sbt" % "sbt-site"       % "1.7.0")
//addSbtPlugin("com.github.sbt" % "sbt-release"    % "1.5.0")
//addSbtPlugin("com.github.sbt" % "sbt-ghpages"    % "0.9.0")
addSbtPlugin("org.scoverage"  % "sbt-scoverage"  % "2.4.4")
addSbtPlugin("org.scoverage"  % "sbt-coveralls"  % "1.3.15")
//addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.11.2")
addSbtPlugin("com.github.sbt" % "sbt-dynver"   % "5.1.1")
addSbtPlugin("com.github.sbt" % "sbt-git"      % "2.1.0")
// scala-xml issues
// TODO remove when everything has migrated to scala-xml 2.x
// https://github.com/scala/bug/issues/12632
ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)

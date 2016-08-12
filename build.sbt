import ReleaseTransformations._
import GhPagesKeys._

organization in ThisBuild := "io.waylay.kairosdb"

lazy val repoSettings = Seq(
  publishTo := {
    if (isSnapshot.value)
      Some("Sonatype snapshot repo" at "https://oss.sonatype.org/content/repositories/snapshots")
    else
      Some("Sonatype releases repo" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
  }
)

val playVersion = "2.5.4"
val akkaVersion = "2.4.8"
val specs2Version = "3.7.3"

val exclusions = Seq("netty-codec", "netty-handler-proxy", "netty-handler", "netty-transport-native-epoll",
  "netty-codec-socks", "netty-codec-http").map(name => ExclusionRule(organization = "io.netty", name = name))

lazy val root = (project in file("."))
  .settings(
    name := "kairosdb-scala",
    scalaVersion := "2.11.8",
    repoSettings,

    parallelExecution in IntegrationTest := false,

    // Be wary of adding extra dependencies (especially the Waylay common dependencies)
    // They may pull in a newer Netty version, breaking play-ws
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % playVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,


      "com.typesafe.play" %% "play-ws" % playVersion,
      "com.netaporter" %% "scala-uri" % "0.4.14",

      // TEST
      "org.specs2" %% "specs2-core" % specs2Version % Test,
      "org.specs2" %% "specs2-junit" % specs2Version % Test,
      "de.leanovate.play-mockws" %% "play-mockws" % "2.5.0"  % Test,
      "com.typesafe.play" %% "play-test" % playVersion % Test, // play-mockws depends on some types in this dependency


      // INTEGRATION TESTS
      // TODO investigate if we can do this with specs2
      "org.scalatest" %% "scalatest" % "2.2.6" % IntegrationTest,
      "com.whisk" %% "docker-testkit-scalatest" % "0.9.0-M5" % IntegrationTest excludeAll(exclusions:_*)
    ),
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-encoding", "UTF-8", // yes, this is 2 args
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-unchecked",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Xfuture"
    )
  )
  .configs(IntegrationTest).settings(Defaults.itSettings: _*)


ghpages.settings
enablePlugins(SiteScaladocPlugin)

val publishScalaDoc = (ref: ProjectRef) => ReleaseStep(
  action = releaseStepTaskAggregated(GhPagesKeys.pushSite in ref) // publish release notes
)

val runIntegrationTest = (ref: ProjectRef) => ReleaseStep(
  action = releaseStepTaskAggregated(test in IntegrationTest in ref)
)

releaseProcess <<= thisProjectRef apply { ref =>
  import sbtrelease.ReleaseStateTransformations._

  Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    runIntegrationTest(ref),
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    publishScalaDoc(ref),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
}

git.remoteRepo := "git@github.com:waylayio/kairosdb-scala.git"

lazy val examples = project
  .dependsOn(root)
  .settings(scalaVersion := "2.11.8")

import sbt.Keys.thisProjectRef

organization in ThisBuild := "io.waylay.kairosdb"

val playWsVersion = "2.0.7"
val playJsonVersion = "2.7.4"
val akkaVersion = "2.5.23"
val specs2Version = "4.8.1"
val dockerTestkitVersion = "0.10.0-beta8"
val scalaTestVersion = "3.0.8"
val playVersion = "2.7.3" // test only

val scala2_12 = "2.12.10"
val scala2_13 = "2.13.1"

ThisBuild / scalaVersion := scala2_13
ThisBuild / crossScalaVersions := Seq(scala2_12, scala2_13)

releaseCrossBuild := true

// we need both Test and IntegrationTest scopes for a correct pom, see https://github.com/sbt/sbt/issues/1380
val TestAndIntegrationTest = IntegrationTest.name + "," + Test.name

val exclusions = Seq(
  "netty-codec",
  "netty-handler-proxy",
  "netty-handler",
  "netty-transport-native-epoll",
  "netty-codec-socks",
  "netty-codec-http"
).map(name => ExclusionRule(organization = "io.netty", name = name))

lazy val root = (project in file("."))
  .settings(
    name := "kairosdb-scala",
    fork in Test := true,
    parallelExecution in IntegrationTest := false,
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.2",
      "com.typesafe.play" %% "play-json" % playJsonVersion,
      "com.typesafe.play" %% "play-ws-standalone" % playWsVersion,
      "com.typesafe.play" %% "play-ws-standalone-json" % playWsVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "io.lemonlabs" %% "scala-uri" % "1.5.1",
      // TEST
      "org.specs2" %% "specs2-core" % specs2Version % Test,
      "org.specs2" %% "specs2-junit" % specs2Version % Test,
      "de.leanovate.play-mockws" %% "play-mockws" % "2.7.1" % Test,
      "com.typesafe.play" %% "play-ahc-ws" % playVersion % TestAndIntegrationTest, // neede for play-mockws
      "com.typesafe.play" %% "play-test" % playVersion % TestAndIntegrationTest, // play-mockws depends on some types in this dependency
      "com.typesafe.play" %% "play-ahc-ws-standalone" % playWsVersion % TestAndIntegrationTest,
      // INTEGRATION TESTS
      // TODO investigate if we can do this with specs2
      "org.scalatest" %% "scalatest" % scalaTestVersion % TestAndIntegrationTest,
      "com.whisk" %% "docker-testkit-scalatest" % dockerTestkitVersion % TestAndIntegrationTest excludeAll (exclusions: _*)
    ),
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-encoding",
      "UTF-8", // yes, this is 2 args
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-unchecked",
      "-Xlint",
      "-Ywarn-dead-code"
    )
  )
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)

enablePlugins(GhpagesPlugin)
enablePlugins(SiteScaladocPlugin)

val publishScalaDoc = (ref: ProjectRef) =>
  ReleaseStep(
    action = releaseStepTaskAggregated(ghpagesPushSite in ref) // publish scaladoc
)

val runIntegrationTest = (ref: ProjectRef) =>
  ReleaseStep(
    action = releaseStepTaskAggregated(test in IntegrationTest in ref)
)

releaseProcess := {
  import sbtrelease.ReleaseStateTransformations._

  Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    runIntegrationTest(thisProjectRef.value),
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    publishScalaDoc(thisProjectRef.value),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
}

git.remoteRepo := "git@github.com:waylayio/kairosdb-scala.git"

lazy val examples = project
  .dependsOn(root)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-ahc-ws-standalone" % playWsVersion
    )
  )

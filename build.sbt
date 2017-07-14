import sbt.Keys.thisProjectRef

organization in ThisBuild := "io.waylay.kairosdb"

val playVersion = "2.6.1"
val akkaVersion = "2.5.3"
val specs2Version = "3.9.2"
val dockerTestkitVersion = "0.9.4"
val scalaTestVersion = "3.0.1"

val scala2_11 = "2.11.8"
val scala2_12 = "2.12.1"

scalaVersion := scala2_12
crossScalaVersions := Seq(scala2_11, scala2_12)

releaseCrossBuild := true

// we need both Test and IntegrationTest scopes for a correct pom, see https://github.com/sbt/sbt/issues/1380
val TestAndIntegrationTest = IntegrationTest.name + "," + Test.name

val exclusions = Seq(
  "netty-codec",
  "netty-handler-proxy",
  "netty-handler",
  "netty-transport-native-epoll",
  "netty-codec-socks",
  "netty-codec-http").map(name => ExclusionRule(organization = "io.netty", name = name))

lazy val root = (project in file("."))
  .settings(
    name := "kairosdb-scala",

    fork in Test := true,
    parallelExecution in IntegrationTest := false,

    // Be wary of adding extra dependencies (especially the Waylay common dependencies)
    // They may pull in a newer Netty version, breaking play-ws
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % playVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,


      "com.typesafe.play" %% "play-ws" % playVersion,
      "io.lemonlabs" %% "scala-uri" % "0.4.16",

      // TEST
      "org.specs2" %% "specs2-core" % specs2Version % Test,
      "org.specs2" %% "specs2-junit" % specs2Version % Test,
      "de.leanovate.play-mockws" %% "play-mockws" % "2.6.0"  % Test,
      "com.typesafe.play" %% "play-test" % playVersion % TestAndIntegrationTest, // play-mockws depends on some types in this dependency
      "com.typesafe.play" %% "play-ahc-ws" % playVersion % TestAndIntegrationTest,

      // INTEGRATION TESTS
      // TODO investigate if we can do this with specs2
      "org.scalatest" %% "scalatest" % scalaTestVersion % TestAndIntegrationTest,
      "com.whisk" %% "docker-testkit-scalatest" % dockerTestkitVersion % TestAndIntegrationTest excludeAll(exclusions:_*),
      "com.whisk" %% "docker-testkit-impl-spotify" % dockerTestkitVersion % TestAndIntegrationTest excludeAll(exclusions:_*)
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
  action = releaseStepTaskAggregated(GhPagesKeys.pushSite in ref) // publish scaladoc
)

val runIntegrationTest = (ref: ProjectRef) => ReleaseStep(
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
  .settings(scalaVersion := "2.11.8")

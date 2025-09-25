import sbt.Keys.thisProjectRef

ThisBuild / organization := "io.waylay"
ThisBuild / homepage     := Some(url("https://waylay.io"))
ThisBuild / developers := List(
  Developer(
    "ramazanyich",
    "Ramil Israfilov",
    "ramazanyich@gmail.com",
    url("https://github.com/ramazanyich")
  ),
  Developer("brunoballekens", "Bruno Ballekens", "bruno@waylay.io", url("https://github.com/brunoballekens"))
)
ThisBuild / licenses := List("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php"))
val playWsVersion   = "3.0.8"
val playJsonVersion = "3.0.5"
val specs2Version   = "4.22.0"
val pekkoVersion    = "1.2.0"

val testContainersVersion = "0.43.0"
val scalaTestVersion      = "3.2.19"
val playVersion           = "3.0.9" // test only

val scala2_13 = "2.13.16"

ThisBuild / scalaVersion := scala2_13

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
    name                                := "kairosdb-scala",
    Test / fork                         := true,
    IntegrationTest / parallelExecution := false,
    libraryDependencies ++= Seq(
      "org.scala-lang.modules"       %% "scala-collection-compat" % "2.13.0",
      "com.fasterxml.jackson.module" %% "jackson-module-scala"    % "2.20.0",
      "org.playframework"            %% "play-json"               % playJsonVersion,
      "org.playframework"            %% "play-ws-standalone"      % playWsVersion,
      "org.playframework"            %% "play-ws-standalone-json" % playWsVersion,
      "com.typesafe.scala-logging"   %% "scala-logging"           % "3.9.6",
      "com.indoorvivants"            %% "scala-uri"               % "4.2.0",
      // TEST
      "org.specs2"               %% "specs2-core"     % specs2Version % Test,
      "org.specs2"               %% "specs2-junit"    % specs2Version % Test,
      "de.leanovate.play-mockws" %% "play-mockws-3-0" % "3.1.0"       % Test,
      "org.playframework" %% "play-ahc-ws" % playVersion % TestAndIntegrationTest, // needed for play-mockws
      "org.playframework" %% "play-test" % playVersion % TestAndIntegrationTest, // play-mockws depends on some types in this dependency
      "org.playframework" %% "play-ahc-ws-standalone"      % playWsVersion % TestAndIntegrationTest,
      "org.apache.pekko"  %% "pekko-actor-typed"           % pekkoVersion  % TestAndIntegrationTest,
      "org.apache.pekko"  %% "pekko-serialization-jackson" % pekkoVersion  % TestAndIntegrationTest,
      "org.apache.pekko"  %% "pekko-slf4j"                 % pekkoVersion  % TestAndIntegrationTest,
      // INTEGRATION TESTS
      // TODO investigate if we can do this with specs2
      "org.scalatest" %% "scalatest-wordspec"             % scalaTestVersion      % TestAndIntegrationTest,
      "org.scalatest" %% "scalatest-mustmatchers"         % scalaTestVersion      % TestAndIntegrationTest,
      "com.dimafeng"  %% "testcontainers-scala-scalatest" % testContainersVersion % TestAndIntegrationTest
    ),
    dependencyOverrides ++= Seq(
      "org.apache.pekko" %% "pekko-actor"                 % pekkoVersion,
      "org.apache.pekko" %% "pekko-actor-typed"           % pekkoVersion,
      "org.apache.pekko" %% "pekko-serialization-jackson" % pekkoVersion,
      "org.apache.pekko" %% "pekko-slf4j"                 % pekkoVersion,
      "org.apache.pekko" %% "pekko-stream"                % pekkoVersion,
      "org.apache.pekko" %% "pekko-protobuf-v3"           % pekkoVersion
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
    action = releaseStepTaskAggregated(ref / ghpagesPushSite)
  )

val runIntegrationTest = (ref: ProjectRef) =>
  ReleaseStep(
    action = releaseStepTaskAggregated(ref / IntegrationTest / test)
  )

releaseProcess := {
  import sbtrelease.ReleaseStateTransformations.*

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
      "org.playframework" %% "play-ahc-ws-standalone" % playWsVersion
    )
  )

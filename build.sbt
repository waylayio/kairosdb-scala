import sbt.Keys.thisProjectRef

ThisBuild / organization := "io.waylay"
ThisBuild / homepage     := Some(uri("https://waylay.io"))
ThisBuild / developers := List(
  Developer(
    "ramazanyich",
    "Ramil Israfilov",
    "ramazanyich@gmail.com",
    uri("https://github.com/ramazanyich")
  ),
  Developer("brunoballekens", "Bruno Ballekens", "bruno@waylay.io", uri("https://github.com/brunoballekens"))
)
ThisBuild / licenses := List("MIT License" -> uri("http://www.opensource.org/licenses/mit-license.php"))
val playWsVersion   = "3.0.13"
val playJsonVersion = "3.0.6"
val specs2Version   = "4.23.0"
val pekkoVersion    = "1.6.0"

val testContainersVersion = "0.44.1"
val scalaTestVersion      = "3.2.20"
val playVersion           = "3.0.11" // test only

val scala2_13 = "2.13.18"

ThisBuild / scalaVersion := scala2_13
ThisBuild / versionScheme           := Some("semver-spec")
ThisBuild / dynverSonatypeSnapshots := true

ThisBuild / libraryDependencySchemes +=
  "org.scala-lang.modules" %% "scala-parser-combinators" % VersionScheme.Always

val exclusions = Seq(
  "netty-codec",
  "netty-handler-proxy",
  "netty-handler",
  "netty-transport-native-epoll",
  "netty-codec-socks",
  "netty-codec-http"
).map(name => ExclusionRule(organization = "io.netty", name = name))

lazy val repoSettings = Seq(
  publishTo := {
    val nexus = "https://nexus.waylay.io"
    if (isSnapshot.value)
      Some("Waylay snapshot repo" at nexus + "/repository/maven-snapshots")
    else
      Some("Waylay releases repo" at nexus + "/repository/maven-releases")
  }
)

lazy val testDependencies = Seq(
  "org.specs2"               %% "specs2-core"     % specs2Version % Test,
  "org.specs2"               %% "specs2-junit"    % specs2Version % Test,
  "de.leanovate.play-mockws" %% "play-mockws-3-0" % "3.1.3"       % Test,
  "org.playframework" %% "play-ahc-ws" % playVersion % Test, // needed for play-mockws
  "org.playframework" %% "play-test" % playVersion % Test, // play-mockws depends on some types in this dependency
  "org.playframework" %% "play-ahc-ws-standalone"      % playWsVersion % Test,
  "org.apache.pekko"  %% "pekko-actor-typed"           % pekkoVersion  % Test,
  "org.apache.pekko"  %% "pekko-serialization-jackson" % pekkoVersion  % Test,
  "org.apache.pekko"  %% "pekko-slf4j"                 % pekkoVersion  % Test,
  "org.scalatest"     %% "scalatest-wordspec"           % scalaTestVersion % Test,
  "org.scalatest"     %% "scalatest-mustmatchers"       % scalaTestVersion % Test,
  "com.dimafeng"      %% "testcontainers-scala-scalatest" % testContainersVersion % Test
)

lazy val pekkoDependencyOverrides = Seq(
  "org.apache.pekko" %% "pekko-actor"                 % pekkoVersion,
  "org.apache.pekko" %% "pekko-actor-typed"           % pekkoVersion,
  "org.apache.pekko" %% "pekko-serialization-jackson" % pekkoVersion,
  "org.apache.pekko" %% "pekko-slf4j"                 % pekkoVersion,
  "org.apache.pekko" %% "pekko-stream"                % pekkoVersion,
  "org.apache.pekko" %% "pekko-protobuf-v3"           % pekkoVersion
)

lazy val root = (project in file("."))
  .settings(
    name        := "kairosdb-scala",
    Test / fork := true,
    libraryDependencies ++= Seq(
      "org.scala-lang.modules"       %% "scala-collection-compat" % "2.14.0",
      "com.fasterxml.jackson.module" %% "jackson-module-scala"    % "2.22.1",
      "org.playframework"            %% "play-json"               % playJsonVersion,
      "org.playframework"            %% "play-ws-standalone"      % playWsVersion,
      "org.playframework"            %% "play-ws-standalone-json" % playWsVersion,
      "com.typesafe.scala-logging"   %% "scala-logging"           % "3.9.6",
      "com.indoorvivants"            %% "scala-uri"               % "4.2.0"
    ) ++ testDependencies,
    dependencyOverrides ++= pekkoDependencyOverrides,
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
    ),
    releaseNotesURL := scmInfo.value.map(scm => uri(s"${scm.browseUrl}/releases"))
  )
  .settings(repoSettings)

lazy val integration = (project in file("integration"))
  .dependsOn(root)
  .settings(
    name                     := "kairosdb-scala-integration-tests",
    publish / skip           := true,
    Test / parallelExecution := false,
    Test / baseDirectory     := (LocalRootProject / baseDirectory).value,
    Test / scalaSource       := (LocalRootProject / baseDirectory).value / "src" / "it" / "scala",
    Test / resourceDirectory := (LocalRootProject / baseDirectory).value / "src" / "it" / "resources",
    libraryDependencies ++= testDependencies,
    dependencyOverrides ++= pekkoDependencyOverrides
  )

git.remoteRepo := "git@github.com:waylayio/kairosdb-scala.git"

lazy val examples = project
  .dependsOn(root)
  .settings(
    libraryDependencies ++= Seq(
      "org.playframework" %% "play-ahc-ws-standalone" % playWsVersion
    )
  )

releasePublishArtifactsAction := PgpKeys.publishSigned.value


pomIncludeRepository := { _ => false }


updateOptions := updateOptions.value.withGigahorse(false)

pomExtra := (
  <url>https://github.com/waylayio/kairosdb-scala</url>
    <licenses>
      <license>
        <name>MIT License</name>
        <url>http://www.opensource.org/licenses/mit-license.php</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <developers>
      <developer>
        <id>francisdb</id>
        <name>Francis De Brabandere</name>
        <url>https://github.com/francisdb</url>
      </developer>
      <developer>
        <id>thomastoye</id>
        <name>Thomas Toye</name>
        <url>https://github.com/thomastoye</url>
      </developer>
      <developer>
        <id>brunoballekens</id>
        <name>Bruno Ballekens</name>
        <url>https://github.com/brunoballekens</url>
      </developer>
    </developers>)

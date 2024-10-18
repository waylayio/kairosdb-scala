//releasePublishArtifactsAction := PgpKeys.publishSigned.value

pomIncludeRepository := { _ => false }

updateOptions := updateOptions.value.withGigahorse(false)

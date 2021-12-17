package integration

import java.nio.file.Paths

import com.spotify.docker.client.messages.HostConfig
import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.KairosDB.KairosDBResponseException
import io.waylay.kairosdb.driver.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.immutable.Seq

class AuthSpec extends IntegrationSpec {

  // TODO enabling auth by providing env variables, when kairosdb 1.1.2 is released
  //  // same as KairosDB Main.java
  //  def toEnvVarName(propName: String) = {
  //    propName.toUpperCase().replace('.', '_')
  //  }
  //
  //  override lazy val dockerEnv = Seq(
  //    toEnvVarName("kairosdb.jetty.basic_auth.user") + "=test",
  //    toEnvVarName("kairosdb.jetty.basic_auth.password") + "=test"
  //  )

  // enabling auth by providing a properties file
  override lazy val volumes = Seq(
    HostConfig.Bind
      .from(Paths.get("src/it/resources/conf").toAbsolutePath.toString)
      .to("/opt/kairosdb/conf")
      .build()
  )

  "The health status" should {
    "fail without auth" in {
      val kairosDB = new KairosDB(wsClient, KairosDBConfig(port = kairosPort), global)
      val res = kairosDB.healthStatus.failed.futureValue

      res mustBe an[KairosDBResponseException]
      res must be(KairosDBResponseException(401, "Unauthorized", Seq.empty))
    }

    "succeed with auth" in {
      val kairosConfig = KairosDBConfig(
        port = kairosPort,
        username = Some("test"),
        password = Some("test")
      )
      val kairosDB = new KairosDB(wsClient, kairosConfig, global)
      val res = kairosDB.healthStatus.futureValue

      res must be(HealthStatusResults(Seq("JVM-Thread-Deadlock: OK", "Datastore-Query: OK")))
    }
  }
}

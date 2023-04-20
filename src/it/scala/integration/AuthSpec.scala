package integration

import java.nio.file.Paths

import com.spotify.docker.client.messages.HostConfig
import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.KairosDB.KairosDBResponseException
import io.waylay.kairosdb.driver.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.immutable.Seq

class AuthSpec extends IntegrationSpec {
  // enabling auth by providing a properties file
  override lazy val volumes = Seq(
    HostConfig.Bind
      .from(Paths.get("src/it/resources/conf/auth").toAbsolutePath.toString)
      .to("/opt/kairosdb/conf/auth")
      .build()
  )

  override lazy val env: Seq[String] = Seq(s"JAVA_OPTS=-Djava.security.auth.login.config=/opt/kairosdb/conf/auth/basicAuth.conf -Dkairosdb.jetty.auth_module_name=basicAuth")


  "The health status" should {
    "fail without auth" in {
      val kairosDB = new KairosDB(wsClient, KairosDBConfig(port = kairosPort), global)
      val res = kairosDB.version.failed.futureValue

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
      val res = kairosDB.version.futureValue

      res must startWith("KairosDB")
    }
  }
}

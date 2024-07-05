package integration

import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.KairosDB.KairosDBResponseException
import io.waylay.kairosdb.driver.models._

import scala.concurrent.ExecutionContext.Implicits.global

class AuthSpec extends IntegrationSpec {

  "The health status" should {
    "fail without auth" in {
      val kairosDB = new KairosDB(
        wsClient,
        KairosDBConfig(port = kairosPort),
        global
      )
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
      val res      = kairosDB.version.futureValue

      res must startWith("KairosDB")
    }
  }
}

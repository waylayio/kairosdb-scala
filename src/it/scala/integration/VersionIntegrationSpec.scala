package integration

import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.models.KairosDBConfig

import scala.concurrent.ExecutionContext.Implicits.global

class VersionIntegrationSpec extends IntegrationSpec {
  "Getting the KairosDB version" should "return version" in {
    val kairosDb = new KairosDB(wsClient, KairosDBConfig(port = kairosPort), global)
    val res = kairosDb.version.futureValue

    res should startWith("KairosDB")
  }
}

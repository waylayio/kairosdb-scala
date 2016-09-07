package integration

import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.models.KairosDBConfig

import scala.concurrent.ExecutionContext.global

class VersionIntegrationSpec extends IntegrationSpec {
  "Getting the KairosDB version" should "return version" in {
    val res = kairosPort.flatMap { kairosPort =>
      new KairosDB(wsClient, KairosDBConfig(port = kairosPort), global).version
    }.futureValue

    res shouldBe "KairosDB 1.1.1-1.20151207194217"
  }
}

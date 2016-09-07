package integration

import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.models.HealthCheckResult.AllHealthy
import io.waylay.kairosdb.driver.models._

import scala.concurrent.ExecutionContext.global

class HealthStatusIntegrationSpec extends IntegrationSpec {
  "The health endpoint" should "for /check return all healthy" in {
    val res = kairosPort.flatMap { kairosPort =>
      val kairosDB = new KairosDB(wsClient, KairosDBConfig(port = kairosPort), global)
      kairosDB.healthCheck
    }.futureValue

    res should be(AllHealthy)
  }

  it should "for /status respond that there are no thread deadlocks and datastore query works" in {
    val res = kairosPort.flatMap { kairosPort =>
      val kairosDB = new KairosDB(wsClient, KairosDBConfig(port = kairosPort), global)
      kairosDB.healthStatus
    }.futureValue

    res should be(HealthStatusResults(Seq("JVM-Thread-Deadlock: OK", "Datastore-Query: OK")))
  }
}

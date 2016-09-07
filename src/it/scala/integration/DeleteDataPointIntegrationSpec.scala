package integration

import java.time.Instant

import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.Implicits._
import io.waylay.kairosdb.driver.models.KairosCompatibleType.KNumber
import io.waylay.kairosdb.driver.models.KairosQuery.QueryTag
import io.waylay.kairosdb.driver.models.QueryResponse.{ResponseQuery, Result, TagResult}
import io.waylay.kairosdb.driver.models._

import scala.concurrent.ExecutionContext._

class DeleteDataPointIntegrationSpec extends IntegrationSpec {

  "Inserting, deleting and then querying data points" should "return empty seq" in {
    val instant = Instant.ofEpochSecond(1470837457L)
    val start = Instant.ofEpochSecond(1470830000L)
    val qm = QueryMetrics(Seq(Query("my.new.metric", QueryTag("aoeu" -> "snth"))), start)

    val res = kairosPort.flatMap { kairosPort: Int =>
      val kairosDB = new KairosDB(wsClient, KairosDBConfig(port = kairosPort), global)

      kairosDB.addDataPoint(DataPoint(MetricName("my.new.metric"), KNumber(555), instant, Seq(Tag("aoeu", "snth")))) flatMap { _ =>
        kairosDB.deleteDataPoints(qm)
      } flatMap { _ =>
        kairosDB.queryMetrics(qm)
      }
    }.futureValue

    res should be(QueryResponse.Response(Seq(ResponseQuery(0, Seq(
      Result("my.new.metric", Seq.empty, Seq.empty, Seq.empty )
    )))))
  }
}

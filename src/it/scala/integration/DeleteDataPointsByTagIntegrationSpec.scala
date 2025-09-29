package integration

import java.time.Instant

import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.Implicits._
import io.waylay.kairosdb.driver.models.KairosCompatibleType.KNumber
import io.waylay.kairosdb.driver.models.KairosQuery.QueryTag
import io.waylay.kairosdb.driver.models.QueryResponse.{ResponseQuery, Result, TagResult}
import io.waylay.kairosdb.driver.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.immutable.Seq

class DeleteDataPointsByTagIntegrationSpec extends IntegrationSpec {

  "Inserting, deleting and then querying data points" should {

    "only return data points that weren't matched by delete" in {
      val instant = Instant.ofEpochSecond(1470837457L)
      val start   = Instant.ofEpochSecond(1470830000L)
      val delete  = QueryMetrics(Seq(Query("my.new.metric", QueryTag("aoeu", Seq("123", "456")))), start)
      val qm      = QueryMetrics(Seq(Query("my.new.metric")), start)

      val dps = Seq(
        DataPoint(MetricName("my.new.metric"), KNumber(111), instant.plusMillis(1), Seq(Tag("aoeu", "123"))),
        DataPoint(MetricName("my.new.metric"), KNumber(222), instant.plusMillis(2), Seq(Tag("snth", "321"))),
        DataPoint(MetricName("my.new.metric"), KNumber(333), instant.plusMillis(3), Seq(Tag("aoeu", "456"))),
        DataPoint(MetricName("my.new.metric"), KNumber(444), instant.plusMillis(4), Seq(Tag("htns", "888"))),
        DataPoint(MetricName("my.new.metric"), KNumber(123), instant.plusMillis(5), Seq(Tag("htns", "999"))),
        DataPoint(MetricName("my.other.metric"), KNumber(555), instant.plusMillis(6), Seq(Tag("aoeu", "snth")))
      )

      val kairosDB = new KairosDB(
        wsClient,
        KairosDBConfig(port = kairosPort, username = Some("test"), password = Some("test")),
        global
      )

      val res = kairosDB
        .addDataPoints(dps)
        .flatMap { _ =>
          kairosDB.deleteDataPoints(delete)
        }
        .flatMap { _ =>
          kairosDB.queryMetrics(qm)
        }
        .futureValue

      res must be(
        QueryResponse.Response(
          Seq(
            ResponseQuery(
              3,
              Seq(
                Result(
                  "my.new.metric",
                  Seq(GroupBy.GroupByType("number")),
                  Seq(
                    TagResult("htns", Seq("888", "999")),
                    TagResult("snth", Seq("321"))
                  ), // not sure if the order is deteministic. Convert to Set?
                  Seq(
                    (instant.plusMillis(2), KNumber(222), None),
                    (instant.plusMillis(4), KNumber(444), None),
                    (instant.plusMillis(5), KNumber(123), None)
                  )
                )
              )
            )
          )
        )
      )
    }
  }
}

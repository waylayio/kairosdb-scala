package integration

import java.time.Instant

import io.waylay.kairosdb.driver.Implicits._
import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.models.KairosCompatibleType.KString
import io.waylay.kairosdb.driver.models.KairosQuery.QueryTag
import io.waylay.kairosdb.driver.models.QueryResponse.{ResponseQuery, Result, TagResult}
import io.waylay.kairosdb.driver.models._

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global

class StringDataPointsIntegrationSpec extends IntegrationSpec {

  "Adding data points and then querying them" should {

    "work for a single string data point" in {
      val instant = Instant.ofEpochSecond(1470837457L)
      val start   = Instant.ofEpochSecond(1470830000L)
      val qm      = QueryMetrics(Seq(Query("my.new.metric", QueryTag("aoeu" -> "snth"))), start)

      val kairosDB = new KairosDB(
        wsClient,
        KairosDBConfig(port = kairosPort, username = Some("test"), password = Some("test")),
        global
      )

      val res = kairosDB
        .addDataPoint(
          DataPoint(MetricName("my.new.metric"), KString("my test string"), instant, Seq(Tag("aoeu", "snth")))
        )
        .flatMap { _ =>
          kairosDB.queryMetrics(qm)
        }
        .futureValue

      res must be(
        QueryResponse.Response(
          Seq(
            ResponseQuery(
              1,
              Seq(
                Result(
                  "my.new.metric",
                  Seq(GroupBy.GroupByType("text")),
                  Seq(TagResult("aoeu", Seq("snth"))),
                  Seq((instant, KString("my test string")))
                )
              )
            )
          )
        )
      )
    }

    "also work for a string with only numbers" in {
      val instant = Instant.ofEpochSecond(1470837457L)
      val start   = Instant.ofEpochSecond(1470830000L)
      val qm      = QueryMetrics(Seq(Query("my.new.metric", QueryTag("aoeu" -> "snth"))), start)

      val kairosDB = new KairosDB(
        wsClient,
        KairosDBConfig(port = kairosPort, username = Some("test"), password = Some("test")),
        global
      )

      val res = kairosDB
        .addDataPoint(DataPoint(MetricName("my.new.metric"), KString("12345"), instant, Seq(Tag("aoeu", "snth"))))
        .flatMap { _ =>
          kairosDB.queryMetrics(qm)
        }
        .futureValue

      res must be(
        QueryResponse.Response(
          Seq(
            ResponseQuery(
              1,
              Seq(
                Result(
                  "my.new.metric",
                  Seq(GroupBy.GroupByType("text")),
                  Seq(TagResult("aoeu", Seq("snth"))),
                  Seq((instant, KString("12345")))
                )
              )
            )
          )
        )
      )
    }
  }
}

package integration

import java.time.{Duration, Instant}

import io.waylay.kairosdb.driver.Implicits._
import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.models.KairosCompatibleType.{KNull, KNumber}
import io.waylay.kairosdb.driver.models.KairosQuery.QueryTag
import io.waylay.kairosdb.driver.models.QueryResponse.{ResponseQuery, Result, TagResult}
import io.waylay.kairosdb.driver.models.RangeAggregator.Align
import io.waylay.kairosdb.driver.models.{DataPoint, _}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

class AddAndQueryDataPointsIntegrationSpec extends IntegrationSpec {

  "Adding data points and then querying them" should "work for a single data point" in {
    val instant = Instant.ofEpochSecond(1470837457L)
    val start = Instant.ofEpochSecond(1470830000L)
    val qm = QueryMetrics(Seq(Query("my.new.metric", QueryTag("aoeu" -> "snth"))), start)

    val res = for{
      port   <- kairosPort
      kairosDB = new KairosDB(wsClient, KairosDBConfig(port = port), global)
      _      <- kairosDB.addDataPoint(DataPoint(MetricName("my.new.metric"), KNumber(555), instant, Seq(Tag("aoeu", "snth"))))
      result <- kairosDB.queryMetrics(qm)
    }yield{
      result
    }

    res.futureValue should be(QueryResponse.Response(Seq(ResponseQuery(1, Seq(
      Result("my.new.metric", Seq(GroupBy.GroupByType("number")), Seq(TagResult("aoeu", Seq("snth"))), Seq((instant, KNumber(555))))
    )))))
  }

  it should "work for multiple data points with gzip compression" in {
    val instant = Instant.ofEpochSecond(1470837457L)
    val start = Instant.ofEpochSecond(1470830000L)
    val metric2 = MetricName("my.new.metric2")
    val qm = QueryMetrics(Seq(Query(metric2.name)), start)

    val dps = Seq(
      DataPoint(metric2, KNumber(111), instant.plusMillis(1), Seq(Tag("aoeu", "123"))),
      DataPoint(metric2, KNumber(222), instant.plusMillis(2), Seq(Tag("snth", "321"))),
      DataPoint(metric2, KNumber(333), instant.plusMillis(3), Seq(Tag("aoeu", "456"))),
    )

    val res = for{
      port   <- kairosPort
      kairosDB = new KairosDB(wsClient, KairosDBConfig(port = port), global)
      _      <- kairosDB.addDataPoints(dps, gzip = true)
      result <- kairosDB.queryMetrics(qm)
    }yield{
      result
    }

    res.futureValue should be(
      QueryResponse.Response(
        Seq(
          ResponseQuery(3, Seq(
            Result(
              "my.new.metric2",
              Seq(GroupBy.GroupByType("number")),
              List(TagResult("aoeu", List("123", "456")), TagResult("snth", List("321"))),
              Seq(
                instant.plusMillis(1) -> KNumber(111),
                instant.plusMillis(2) -> KNumber(222),
                instant.plusMillis(3) -> KNumber(333)
              )
            )
          ))
        )
      )
    )
  }

  it should "work for an aggregate query with nulls" in {
    val start = Instant.ofEpochSecond(0)
    val end = start.plus(Duration.ofDays(4))

    val firstPoint = start.plus(Duration.ofDays(1))
    // nulls in between
    val secondPoint = start.plus(Duration.ofDays(3))

    val datapoint = DataPoint(MetricName("my.new.metric"), KNumber(555), firstPoint, Seq(Tag("aoeu", "snth")))

    val qm = QueryMetrics(Seq(
      Query("my.new.metric", QueryTag("aoeu" -> "snth"), aggregators = Seq(
        Aggregator.Average(1.days, align = Some(Align.AlignStartTime)),
        Aggregator.Gaps(1.days, align = Some(Align.AlignStartTime))
      ))
    ), TimeSpan(start, Some(end)))

    val res = for{
      port    <- kairosPort
      kairosDB = new KairosDB(wsClient, KairosDBConfig(port = port), global)
      _       <- kairosDB.addDataPoint(datapoint)
      _       <- kairosDB.addDataPoint(datapoint.copy(timestamp = secondPoint))
      results <- kairosDB.queryMetrics(qm)
    }yield{
      results
    }

    res.futureValue should be(QueryResponse.Response(Seq(ResponseQuery(2, Seq(
      Result("my.new.metric", Seq(GroupBy.GroupByType("number")), Seq(TagResult("aoeu", Seq("snth"))), Seq(
        (Instant.parse("1970-01-02T00:00:00Z"), KNumber(555)),
        (Instant.parse("1970-01-03T00:00:00Z"), KNull),
        (Instant.parse("1970-01-04T00:00:00Z"), KNumber(555)
      ))
    ))))))
  }

}

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

    val res = kairosPort.flatMap { kairosPort =>
      val kairosDB = new KairosDB(wsClient, KairosDBConfig(port = kairosPort), global)

      for{
        _   <- kairosDB.addDataPoint(DataPoint(MetricName("my.new.metric"), KNumber(555), instant, Seq(Tag("aoeu", "snth"))))
        res <- kairosDB.queryMetrics(qm)
      }yield{
        res
      }
    }.futureValue

    res should be(QueryResponse.Response(Seq(ResponseQuery(1, Seq(
      Result("my.new.metric", Seq(GroupBy.GroupByType("number")), Seq(TagResult("aoeu", Seq("snth"))), Seq( (instant, KNumber(555)) ) )
    )))))
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

    val res = kairosPort.flatMap { kairosPort =>
      val kairosDB = new KairosDB(wsClient, KairosDBConfig(port = kairosPort), global)

      for{
        _       <- kairosDB.addDataPoint(datapoint)
        _       <- kairosDB.addDataPoint(datapoint.copy(timestamp = secondPoint))
        results <-  kairosDB.queryMetrics(qm)
      }yield{
        results
      }
    }.futureValue

    res should be(QueryResponse.Response(Seq(ResponseQuery(2, Seq(
      Result("my.new.metric", Seq(GroupBy.GroupByType("number")), Seq(TagResult("aoeu", Seq("snth"))), Seq(
        (Instant.parse("1970-01-02T00:00:00Z"), KNumber(555)),
        (Instant.parse("1970-01-03T00:00:00Z"), KNull),
        (Instant.parse("1970-01-04T00:00:00Z"), KNumber(555)
      ))
    ))))))
  }

}

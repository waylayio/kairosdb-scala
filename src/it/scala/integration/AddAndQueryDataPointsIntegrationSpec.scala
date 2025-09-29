package integration

import java.time.{Duration, Instant}

import io.waylay.kairosdb.driver.Implicits._
import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.models.KairosCompatibleType.{KNull, KNumber}
import io.waylay.kairosdb.driver.models.KairosQuery.QueryTag
import io.waylay.kairosdb.driver.models.QueryResponse.{ResponseQuery, Result, TagResult}
import io.waylay.kairosdb.driver.models.RangeAggregator.Align
import io.waylay.kairosdb.driver.models.{DataPoint, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.collection.immutable.Seq

class AddAndQueryDataPointsIntegrationSpec extends IntegrationSpec {

  "Adding data points and then querying them" should {

    "work for a single data point" in {
      val instant = Instant.ofEpochSecond(1470837457L)
      val start   = Instant.ofEpochSecond(1470830000L)
      val qm      = QueryMetrics(Seq(Query("my.new.metric", QueryTag("aoeu" -> "snth"))), start)
      val kairosDB = new KairosDB(
        wsClient,
        KairosDBConfig(port = kairosPort, username = Some("test"), password = Some("test")),
        global
      )
      val res = for {
        _ <-
          kairosDB.addDataPoint(DataPoint(MetricName("my.new.metric"), KNumber(555), instant, Seq(Tag("aoeu", "snth"))))
        result <- kairosDB.queryMetrics(qm)
      } yield result

      res.futureValue must be(
        QueryResponse.Response(
          Seq(
            ResponseQuery(
              1,
              Seq(
                Result(
                  "my.new.metric",
                  Seq(GroupBy.GroupByType("number")),
                  Seq(TagResult("aoeu", Seq("snth"))),
                  Seq((instant, KNumber(555), None))
                )
              )
            )
          )
        )
      )
    }

    "work for multiple data points with gzip compression" in {
      val instant = Instant.ofEpochSecond(1470837457L)
      val start   = Instant.ofEpochSecond(1470830000L)
      val metric2 = MetricName("my.new.metric2")
      val qm      = QueryMetrics(Seq(Query(metric2.name)), start)

      val dps = Seq(
        DataPoint(metric2, KNumber(111), instant.plusMillis(1), Seq(Tag("aoeu", "123"))),
        DataPoint(metric2, KNumber(222), instant.plusMillis(2), Seq(Tag("snth", "321"))),
        DataPoint(metric2, KNumber(333), instant.plusMillis(3), Seq(Tag("aoeu", "456")))
      )

      val kairosDB = new KairosDB(
        wsClient,
        KairosDBConfig(port = kairosPort, username = Some("test"), password = Some("test")),
        global
      )
      val res = for {
        _      <- kairosDB.addDataPoints(dps, gzip = true)
        result <- kairosDB.queryMetrics(qm)
      } yield result

      res.futureValue must be(
        QueryResponse.Response(
          Seq(
            ResponseQuery(
              3,
              Seq(
                Result(
                  "my.new.metric2",
                  Seq(GroupBy.GroupByType("number")),
                  List(TagResult("aoeu", List("123", "456")), TagResult("snth", List("321"))),
                  Seq(
                    (instant.plusMillis(1), KNumber(111), None),
                    (instant.plusMillis(2), KNumber(222), None),
                    (instant.plusMillis(3), KNumber(333), None)
                  )
                )
              )
            )
          )
        )
      )
    }

    "work for an aggregate query with nulls" in {
      val start = Instant.ofEpochSecond(0)
      val end   = start.plus(Duration.ofDays(4))

      val firstPoint = start.plus(Duration.ofDays(1))
      // nulls in between
      val secondPoint = start.plus(Duration.ofDays(3))

      val datapoint = DataPoint(MetricName("my.new.metric"), KNumber(555), firstPoint, Seq(Tag("aoeu", "snth")))

      val qm = QueryMetrics(
        Seq(
          Query(
            "my.new.metric",
            QueryTag("aoeu" -> "snth"),
            aggregators = Seq(
              Aggregator.Average(1.days, align = Some(Align.AlignStartTime)),
              Aggregator.Gaps(1.days, align = Some(Align.AlignStartTime))
            )
          )
        ),
        TimeSpan(start, Some(end))
      )

      val kairosDB = new KairosDB(
        wsClient,
        KairosDBConfig(port = kairosPort, username = Some("test"), password = Some("test")),
        global
      )

      val res = for {
        _       <- kairosDB.addDataPoint(datapoint)
        _       <- kairosDB.addDataPoint(datapoint.copy(timestamp = secondPoint))
        version <- kairosDB.version
        results <- kairosDB.queryMetrics(qm)
      } yield (version, results)

      res.futureValue match {
        // bug in Kairos < 1.1.4
        // see https://github.com/kairosdb/kairosdb/issues/339
        case ("KairosDB 1.1.3-1.20170102211109", results) =>
          results must be(
            QueryResponse.Response(
              Seq(
                ResponseQuery(
                  2,
                  Seq(
                    Result(
                      "my.new.metric",
                      Seq(GroupBy.GroupByType("number")),
                      Seq(TagResult("aoeu", Seq("snth"))),
                      Seq(
                        (Instant.parse("1970-01-02T00:00:00Z"), KNumber(555), None),
                        (Instant.parse("1970-01-03T00:00:00Z"), KNull, None),
                        (Instant.parse("1970-01-04T00:00:00Z"), KNumber(555), None)
                      )
                    )
                  )
                )
              )
            )
          )
        case ("KairosDB 1.2.0-1.20180201074909", results) =>
          results must be(
            QueryResponse.Response(
              Seq(
                ResponseQuery(
                  2,
                  Seq(
                    Result(
                      "my.new.metric",
                      Seq(GroupBy.GroupByType("number")),
                      Seq(TagResult("aoeu", Seq("snth"))),
                      Seq(
                        (Instant.parse("1970-01-01T00:00:00Z"), KNull, None),
                        (Instant.parse("1970-01-02T00:00:00Z"), KNumber(555), None),
                        (Instant.parse("1970-01-03T00:00:00Z"), KNull, None),
                        (Instant.parse("1970-01-04T00:00:00Z"), KNumber(555), None)
                      )
                    )
                  )
                )
              )
            )
          )
        case ("KairosDB 1.3.0-1.20210808220820", results) =>
          results must be(
            QueryResponse.Response(
              Seq(
                ResponseQuery(
                  2,
                  Seq(
                    Result(
                      "my.new.metric",
                      Seq(GroupBy.GroupByType("number")),
                      Seq(TagResult("aoeu", Seq("snth"))),
                      Seq(
                        (Instant.parse("1970-01-01T00:00:00Z"), KNull, None),
                        (Instant.parse("1970-01-02T00:00:00Z"), KNumber(555), None),
                        (Instant.parse("1970-01-03T00:00:00Z"), KNull, None),
                        (Instant.parse("1970-01-04T00:00:00Z"), KNumber(555), None),
                        (Instant.parse("1970-01-05T00:00:00Z"), KNull, None)
                      )
                    )
                  )
                )
              )
            )
          )
        case other =>
          fail(s"Unknown kairos version ${other._1}")
      }
    }
  }
}

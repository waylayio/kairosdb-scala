package unit

import java.time.Instant

import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.KairosDB._
import io.waylay.kairosdb.driver.models.Aggregator.{Average, Sum}
import io.waylay.kairosdb.driver.models.GroupBy.{GroupByTags, GroupByType}
import io.waylay.kairosdb.driver.models.KairosCompatibleType.KNumber
import io.waylay.kairosdb.driver.models.KairosQuery.QueryTag
import io.waylay.kairosdb.driver.models.QueryMetricTagsResponse.{TagsResponse, TagsResult}
import io.waylay.kairosdb.driver.models.QueryResponse.{Response, ResponseQuery, Result, TagResult}
import io.waylay.kairosdb.driver.models.TimeSpan.{AbsoluteStartTime, RelativeEndTime}
import io.waylay.kairosdb.driver.models._
import mockws.MockWS
import org.specs2.mutable.Specification
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Results._
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class QuerySpec extends Specification {

  "KairosDB#queryMetrics" should {

    "return a correct query response" in { implicit ee: ExecutionEnv =>
      val mockWs = MockWS {
        case ("POST", "http://localhost:8080/api/v1/datapoints/query") => Action { req =>
          val expected = Json.parse(
            """
              |{
              |   "start_absolute": 1357023600000,
              |   "end_relative": {
              |       "value": "5",
              |       "unit": "days"
              |   },
              |   "time_zone": "Asia/Kabul",
              |   "metrics": [
              |       {
              |           "tags": {
              |               "host": ["foo", "foo2"],
              |               "customer": ["bar"]
              |           },
              |           "name": "abc.123",
              |           "limit": 10000,
              |           "aggregators": [
              |               {
              |                   "name": "sum",
              |                   "sampling": {
              |                       "value": "10",
              |                       "unit": "minutes"
              |                   }
              |               }
              |           ]
              |       },
              |       {
              |           "tags": {
              |               "host": ["foo", "foo2"],
              |               "customer": ["bar"]
              |           },
              |           "name": "xyz.123",
              |           "aggregators": [
              |               {
              |                   "name": "avg",
              |                   "sampling": {
              |                       "value": "10",
              |                       "unit": "minutes"
              |                   }
              |               }
              |           ]
              |       }
              |   ]
              |}
            """.stripMargin)
          val response = Json.parse(
            """
              |{
              |  "queries": [
              |      {
              |          "sample_size": 14368,
              |          "results": [
              |              {
              |                  "name": "abc_123",
              |                  "group_by": [
              |                      {
              |                         "name": "type",
              |                         "type": "number"
              |                      },
              |                      {
              |                         "name": "tag",
              |                         "tags": [
              |                             "host"
              |                         ],
              |                        "group": {
              |                             "host": "server1"
              |                        }
              |                      }
              |                  ],
              |                  "tags": {
              |                      "host": [
              |                          "server1"
              |                      ],
              |                      "customer": [
              |                          "bar"
              |                      ]
              |                  },
              |                  "values": [
              |                      [
              |                          1364968800000,
              |                          11019
              |                      ],
              |                      [
              |                          1366351200000,
              |                          2843
              |                      ]
              |                  ]
              |              }
              |         ]
              |     }
              |  ]
              |}
            """.stripMargin)

          req.body.asJson.map(x => if (x == expected) Ok(response) else BadRequest) getOrElse BadRequest
        }
      }

      val kairosDb = new KairosDB(StandaloneMockWs(mockWs), KairosDBConfig(), global)

      val query1 = Query(
        MetricName("abc.123"),
        Seq(QueryTag("host", Seq("foo", "foo2")), QueryTag("customer", Seq("bar"))),
        aggregators = Seq(Sum(10.minutes)),
        limit = Some(10000)
      )

      val query2 = Query(
        MetricName("xyz.123"),
        Seq(QueryTag("host", Seq("foo", "foo2")), QueryTag("customer", Seq("bar"))),
        aggregators = Seq(Average(10.minutes))
      )

      val qm = QueryMetrics(
        Seq(query1, query2),
        TimeSpan(AbsoluteStartTime(Instant.ofEpochMilli(1357023600000L)), Some(RelativeEndTime(5.days))),
        timeZone = Some("Asia/Kabul")
      )

      val expected = Response(Seq(
        ResponseQuery(14368,
          Seq(
            Result(
              MetricName("abc_123"),
              Seq(GroupByType("number"),
                GroupByTags(Seq("host"))), // TODO group by tag in the result has an extra field "group"
              Seq(TagResult("host", Seq("server1")), TagResult("customer", Seq("bar"))),
              Seq((Instant.ofEpochMilli(1364968800000L), KNumber(11019)), (Instant.ofEpochMilli(1366351200000L), KNumber(2843)))
            )
          )
        )
      ))

      val r = kairosDb.queryMetrics(qm) must be_==(expected).await(1, 10.seconds)
      mockWs.close()
      r
    }

    "return a failed future if KairosDB sends unparseable JSON" in { implicit ee: ExecutionEnv =>
      val mockWs = MockWS {
        case ("POST", "http://localhost:8080/api/v1/datapoints/query") => Action { req =>
          val expected = Json.parse(
            """
              |{
              |   "start_absolute": 1357023600000,
              |   "end_relative": {
              |       "value": "5",
              |       "unit": "days"
              |   },
              |   "time_zone": "Asia/Kabul",
              |   "metrics": [
              |       {
              |           "tags": {
              |               "host": ["foo", "foo2"],
              |               "customer": ["bar"]
              |           },
              |           "name": "abc.123",
              |           "limit": 10000,
              |           "aggregators": [
              |               {
              |                   "name": "sum",
              |                   "sampling": {
              |                       "value": "10",
              |                       "unit": "minutes"
              |                   }
              |               }
              |           ]
              |       },
              |       {
              |           "tags": {
              |               "host": ["foo", "foo2"],
              |               "customer": ["bar"]
              |           },
              |           "name": "xyz.123",
              |           "aggregators": [
              |               {
              |                   "name": "avg",
              |                   "sampling": {
              |                       "value": "10",
              |                       "unit": "minutes"
              |                   }
              |               }
              |           ]
              |       }
              |   ]
              |}
            """.stripMargin)
          val response = Json.parse(
            """
              |{}
            """.stripMargin)

          req.body.asJson.map(x => if (x == expected) Ok(response) else BadRequest) getOrElse BadRequest
        }
      }

      val kairosDb = new KairosDB(StandaloneMockWs(mockWs), KairosDBConfig(), global)

      val query1 = Query(
        MetricName("abc.123"),
        Seq(QueryTag("host", Seq("foo", "foo2")), QueryTag("customer", Seq("bar"))),
        aggregators = Seq(Sum(10.minutes)),
        limit = Some(10000)
      )

      val query2 = Query(
        MetricName("xyz.123"),
        Seq(QueryTag("host", Seq("foo", "foo2")), QueryTag("customer", Seq("bar"))),
        aggregators = Seq(Average(10.minutes))
      )

      val qm = QueryMetrics(
        Seq(query1, query2),
        TimeSpan(AbsoluteStartTime(Instant.ofEpochMilli(1357023600000L)), Some(RelativeEndTime(5.days))),
        timeZone = Some("Asia/Kabul")
      )

      val r = kairosDb.queryMetrics(qm) must throwA[KairosDBResponseParseException].await(1, 10.seconds)
      mockWs.close()
      r
    }
  }


  "KairosDB#queryMetricTags" should {

      "return a correct query response" in { implicit ee: ExecutionEnv =>
        val mockWs = MockWS {
          case ("POST", "http://localhost:8080/api/v1/datapoints/query/tags") => Action { req =>
            val expected = Json.parse(
              """
                |{
                |   "start_absolute": 1357023600000,
                |   "end_relative": {
                |       "value": "5",
                |       "unit": "days"
                |   },
                |   "metrics": [
                |       {
                |           "tags": {
                |               "host": ["foo"]
                |           },
                |           "name": "abc.123"
                |       },
                |       {
                |           "tags": {
                |               "host": ["foo"]
                |           },
                |           "name": "xyz.123"
                |       }
                |   ]
                |}
              """.stripMargin)
            val response = Json.parse(
              """
                |{
                |    "results": [
                |        {
                |            "name": "abc_123",
                |            "tags": {
                |                "host": ["server1","server2"],
                |                "type": ["bar"]
                |            }
                |        },
                |        {
                |            "name": "xyz_123",
                |            "tags": {
                |                "host": ["server1","server2"],
                |                "type": ["bar"]
                |            }
                |        }
                |    ]
                |}
              """.stripMargin)

            req.body.asJson.map(x => if(x == expected) Ok(response) else BadRequest) getOrElse BadRequest
          }
        }

        val kairosDb = new KairosDB(StandaloneMockWs(mockWs), KairosDBConfig(), global)

        val query1 = Query(MetricName("abc.123"), Seq(QueryTag("host", Seq("foo"))))
        val query2 = Query(MetricName("xyz.123"), Seq(QueryTag("host", Seq("foo"))))

        val qm = QueryMetrics(
          Seq(query1, query2),
          TimeSpan(AbsoluteStartTime(Instant.ofEpochMilli(1357023600000L)), Some(RelativeEndTime(5.days)))
        )

        val expected = TagsResponse(Seq(
          TagsResult(
            MetricName("abc_123"),
            Seq(TagResult("host", Seq("server1", "server2")), TagResult("type", Seq("bar")))
          ),
          TagsResult(
            MetricName("xyz_123"),
            Seq(TagResult("host", Seq("server1", "server2")), TagResult("type", Seq("bar")))
          )
        ))

        val r = kairosDb.queryMetricTags(qm) must be_==(expected).await(1, 10.seconds)
        mockWs.close()
        r
      }
  }

  "KairosDB#deleteDataPoints" should {

    "send a correct query" in { implicit ee: ExecutionEnv =>
      val mockWs = MockWS {
        case ("POST", "http://localhost:8080/api/v1/datapoints/delete") => Action { req =>
          val expected = Json.parse(
            """
              |{
              |   "start_absolute": 1357023600000,
              |   "end_relative": {
              |       "value": "5",
              |       "unit": "days"
              |   },
              |   "time_zone": "Asia/Kabul",
              |   "metrics": [
              |       {
              |           "tags": {
              |               "host": ["foo", "foo2"],
              |               "customer": ["bar"]
              |           },
              |           "name": "abc.123",
              |           "limit": 10000,
              |           "aggregators": [
              |               {
              |                   "name": "sum",
              |                   "sampling": {
              |                       "value": "10",
              |                       "unit": "minutes"
              |                   }
              |               }
              |           ]
              |       },
              |       {
              |           "tags": {
              |               "host": ["foo", "foo2"],
              |               "customer": ["bar"]
              |           },
              |           "name": "xyz.123",
              |           "aggregators": [
              |               {
              |                   "name": "avg",
              |                   "sampling": {
              |                       "value": "10",
              |                       "unit": "minutes"
              |                   }
              |               }
              |           ]
              |       }
              |   ]
              |}
            """.stripMargin)

          req.body.asJson.map(x => if(x == expected) NoContent else BadRequest) getOrElse BadRequest
        }
      }

      val kairosDb = new KairosDB(StandaloneMockWs(mockWs), KairosDBConfig(), global)

      val query1 = Query(
        MetricName("abc.123"),
        Seq(QueryTag("host", Seq("foo", "foo2")), QueryTag("customer", Seq("bar"))),
        aggregators = Seq(Sum(10.minutes)),
        limit = Some(10000)
      )

      val query2 = Query(
        MetricName("xyz.123"),
        Seq(QueryTag("host", Seq("foo", "foo2")), QueryTag("customer", Seq("bar"))),
        aggregators = Seq(Average(10.minutes))
      )

      val qm = QueryMetrics(
        Seq(query1, query2),
        TimeSpan(AbsoluteStartTime(Instant.ofEpochMilli(1357023600000L)), Some(RelativeEndTime(5.days))),
        timeZone = Some("Asia/Kabul")
      )

      val r = kairosDb.deleteDataPoints(qm) must be_==(()).await(1, 10.seconds)
      mockWs.close()
      r
    }
  }

}

package unit

import java.time.Instant

import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.models.KairosQuery.QueryTag
import io.waylay.kairosdb.driver.models.TimeSpan.{AbsoluteStartTime, RelativeEndTime}
import io.waylay.kairosdb.driver.models._
import io.waylay.kairosdb.driver.Implicits.finiteDuration2timeRange
import mockws.MockWS
import org.specs2.mutable.Specification
import play.api.libs.json.Json
import play.api.mvc.Results._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.{FutureMatchers, ResultMatchers}

import scala.concurrent.duration._
import scala.collection.immutable.Seq


class QueryMetricTagsSpec(implicit ee: ExecutionEnv) extends Specification with FutureMatchers with ResultMatchers with MockHelper {

  "KairosDB#queryMetricTags" should {

    "query metric tags" in {
      skipped { // TODO Response parsing in KairosDB#queryMetricTags is not implemented yet
      val mockWs = MockWS {
        case ("POST", "http://localhost/api/v1/datapoints/query/tags") => Action { req =>
          val expected = Json.parse(
            """{
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
              |       }
              |   ]
              |}
              | """.stripMargin)

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

          println(s"\n\n JSON --> ${req.body.asJson.get}\n\n")
          req.body.asJson.map { json => if (json == expected) Ok(response) else BadRequest } getOrElse BadRequest
        }
      }

      val kairosDb = new KairosDB(StandaloneMockWs(mockWs), KairosDBConfig(), ee.ec)
      val query = Query(MetricName("abc.123"), Seq(QueryTag("host", Seq("foo"))))
      val qm = QueryMetrics(Seq(query), TimeSpan(AbsoluteStartTime(Instant.ofEpochMilli(1357023600000L)), Some(RelativeEndTime(5.days))))
      val expected = QueryResponse.Response(Seq.empty) // TODO

      val r = kairosDb.queryMetricTags(qm) must beEqualTo(expected).await(1, 3.seconds)
      mockWs.close()
      r
    }}
  }
}

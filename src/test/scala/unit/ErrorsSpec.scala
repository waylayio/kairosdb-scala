package unit

import java.time.Instant

import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.KairosDB.{KairosDBResponseBadRequestException, KairosDBResponseInternalServerErrorException, KairosDBResponseUnauthorizedException, KairosDBResponseUnhandledException}
import io.waylay.kairosdb.driver.models.Aggregator.Sum
import io.waylay.kairosdb.driver.models.KairosQuery.QueryTag
import io.waylay.kairosdb.driver.models.TimeSpan.{AbsoluteStartTime, RelativeEndTime}
import io.waylay.kairosdb.driver.models._
import mockws.MockWS
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Results._

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._

class ErrorsSpec extends Specification{

  sequential

  "the shared error handling should" should {

    "handle bad request errors" in { implicit ee: ExecutionEnv =>
      val mockWs = MockWS {
        case ("POST", "http://localhost:8080/api/v1/datapoints/query") => Action { req =>
          val json = Json.parse(
            """
              |{
              |    "errors": [
              |        "metrics[0].aggregate must be one of MIN,SUM,MAX,AVG,DEV",
              |        "metrics[0].sampling.unit must be one of  SECONDS,MINUTES,HOURS,DAYS,WEEKS,YEARS"
              |    ]
              |}
            """.stripMargin
          )
          BadRequest(json)

          // reply to everything with BadRequest, just for testing
        }
      }

      val kairosDb = new KairosDB(StandaloneMockWs(mockWs), KairosDBConfig(), global)
      val query1 = Query(
        MetricName("abc.123"),
        Seq(QueryTag("host", Seq("foo", "foo2")), QueryTag("customer", Seq("bar"))),
        aggregators = Seq(Sum(10.minutes)),
        limit = Some
        (10000)
      )

      val qm = QueryMetrics(
        Seq(query1),
        TimeSpan(AbsoluteStartTime(Instant.ofEpochMilli(1357023600000L)), Some(RelativeEndTime(5.days))),
        timeZone = Some("Asia/Kabul")
      )

      try {
        kairosDb.queryMetrics(qm) must throwAn[KairosDBResponseBadRequestException].await(1, 10.seconds)
      } finally {
        mockWs.close()
      }
    }

    "handle internal server errors" in { implicit ee: ExecutionEnv =>
      val mockWs = MockWS {
        case ("GET", "http://localhost:8080/api/v1/health/status") => Action { req =>
          val json = Json.parse(
            """
              |{
              |    "errors": [
              |        "boom"
              |    ]
              |}
            """.stripMargin)
          // reply to everything with InternalServerError, just for testing
          InternalServerError(json)
        }
      }

      val kairosDb = new KairosDB(StandaloneMockWs(mockWs), KairosDBConfig(), global)
      try {
        kairosDb.healthStatus must throwAn[KairosDBResponseInternalServerErrorException].await(1, 10.seconds)
      }finally {
        mockWs.close()
      }
    }

    "handle authorization server errors" in { implicit ee: ExecutionEnv =>
      val mockWs = MockWS {
        case ("GET", "http://localhost:8080/api/v1/health/status") => Action { req =>
          Unauthorized("<html></html>") // this is a html jetty 401 response
        }
      }

      val kairosDb = new KairosDB(StandaloneMockWs(mockWs), KairosDBConfig(), global)
      try {
        kairosDb.healthStatus must throwAn[KairosDBResponseUnauthorizedException].await(1, 10.seconds)
      }finally{
        mockWs.close()
      }
    }

    "handle any other request errors" in { implicit ee: ExecutionEnv =>
      val mockWs = MockWS {
        case ("GET", "http://localhost:8080/api/v1/health/status") => Action { req =>
          val json = Json.parse(
            """
              |{
              |    "errors": [
              |        "???"
              |    ]
              |}
            """.stripMargin)
          // 414 Request-URI Too Long
          Status(414)(json)
        }
      }

      val kairosDb = new KairosDB(StandaloneMockWs(mockWs), KairosDBConfig(), global)

      try {
        kairosDb.healthStatus must throwAn[KairosDBResponseUnhandledException].await(1, 10.seconds)
      }finally {
        mockWs.close()
      }
    }

  }
}

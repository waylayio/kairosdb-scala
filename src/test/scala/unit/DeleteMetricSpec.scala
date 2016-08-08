package unit

import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.models._
import mockws.MockWS
import org.specs2.mutable.Specification
import play.api.mvc.Action
import play.api.mvc.Results._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.{FutureMatchers, ResultMatchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class DeleteMetricSpec extends Specification with FutureMatchers with ResultMatchers {
  "KairosDB#deleteMetric" should {
    "delete metric" in { implicit ee: ExecutionEnv =>
      val mockWs = MockWS {
        case ("DELETE", "http://localhost:8080/api/v1/metric/my.metric.123") => Action {
          NoContent
        }
      }

      val kairosDb = new KairosDB(mockWs, KairosDBConfig(), global)

      val r = kairosDb.deleteMetric(MetricName("my.metric.123")) must beEqualTo(()).await(1, 3.seconds)
      mockWs.close()
      r
    }
  }
}

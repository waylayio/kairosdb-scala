package unit

import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.models._
import mockws.MockWS
import org.specs2.mutable.Specification
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Results._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.{FutureMatchers, ResultMatchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ListTagValuesSpec extends Specification with FutureMatchers with ResultMatchers {
  "KairosDB#listTagValues" should {
    "return the correct tag values" in { implicit ee: ExecutionEnv =>
      val expected = Seq("mytag", "foo", "bar1")

      val mockWs = MockWS {
        case ("GET", "http://localhost:8080/api/v1/tagvalues") => Action {
          Ok(Json.obj("results" -> expected))
        }
      }

      val kairosDb = new KairosDB(StandaloneMockWs(mockWs), KairosDBConfig(), global)

      val r = kairosDb.listTagValues must be_==(expected).await(1, 3.seconds)
      mockWs.close()
      r
    }
  }
}

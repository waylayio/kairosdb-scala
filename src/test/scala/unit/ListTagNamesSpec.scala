package unit

import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.models._
import mockws.MockWS
import org.specs2.mutable.Specification
import play.api.libs.json.Json
import play.api.mvc.Results._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.{FutureMatchers, ResultMatchers}

import scala.concurrent.duration._

class ListTagNamesSpec(implicit ee: ExecutionEnv) extends Specification with FutureMatchers with ResultMatchers with MockHelper {

  "KairosDB#listTagNames" should {

    "return the correct tag names" in {
      val expected = Seq("mytag", "foo", "bar1")

      val mockWs = MockWS {
        case ("GET", "http://localhost:8080/api/v1/tagnames") => Action {
          Ok(Json.obj("results" -> expected))
        }
      }

      val kairosDb = new KairosDB(StandaloneMockWs(mockWs), KairosDBConfig(), ee.ec)

      val r = kairosDb.listTagNames must be_==(expected).await(1, 3.seconds)
      mockWs.close()
      r
    }
  }
}

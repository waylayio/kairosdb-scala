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
import play.api.libs.ws.{StandaloneWSClient, StandaloneWSRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class VersionSpec extends Specification with FutureMatchers with ResultMatchers {
  "KairosDB#version" should {
    "return the correct version number" in { implicit ee: ExecutionEnv =>
      val mockWs = MockWS {
        case ("GET", "http://localhost:8080/api/v1/version") => Action {
          Ok(Json.obj("version" -> "KairosDB 0.9.4"))
        }
      }

      val kairosDb = new KairosDB(StandaloneMockWs(mockWs), KairosDBConfig(), global)

      val r = kairosDb.version must be_==("KairosDB 0.9.4").await(1, 3.seconds)
      mockWs.close()
      r
    }
  }


  // remove once this is fixed: https://github.com/leanovate/play-mockws/issues/20
  object StandaloneMockWs{
    def apply(mockWs: MockWS) = new StandaloneMockWs(mockWs)
  }
  class StandaloneMockWs(mockWs: MockWS) extends StandaloneWSClient{
    override def underlying[T]: T = mockWs.underlying[T]

    override def url(url: String): StandaloneWSRequest = mockWs.url(url)

    override def close(): Unit = mockWs.close()
  }
}

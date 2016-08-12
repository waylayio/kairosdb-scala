package unit

import io.waylay.kairosdb.driver.models.json.Formats._
import org.specs2.mutable.Specification
import play.api.libs.json.Json

import scala.concurrent.duration._

class FiniteDurationReadsSpec extends Specification {
  "Finite duration reads" should {
    "read milliseconds" in {
      Json.parse("""{"unit":"milliseconds","value":10}""").validate[FiniteDuration].get should be equalTo 10.millis
    }

    "read seconds" in {
      Json.parse("""{"unit":"seconds","value":10}""").validate[FiniteDuration].get should be equalTo 10.seconds
    }

    "read minutes" in {
      Json.parse("""{"unit":"minutes","value":10}""").validate[FiniteDuration].get should be equalTo 10.minutes
    }

    "read hours" in {
      Json.parse("""{"unit":"hours","value":10}""").validate[FiniteDuration].get should be equalTo 10.hours
    }

    "read days" in {
      Json.parse("""{"unit":"days","value":10}""").validate[FiniteDuration].get should be equalTo 10.days
    }

    "read weeks" in {
      Json.parse("""{"unit":"weeks","value":10}""").validate[FiniteDuration].get should be equalTo (10 * 7).days
    }

    "fail to read months" in {
      Json.parse("""{"unit":"months","value":10}""").validate[FiniteDuration].isError should beTrue
    }

    "fail to read years" in {
      Json.parse("""{"unit":"years","value":10}""").validate[FiniteDuration].isError should beTrue
    }
  }

}

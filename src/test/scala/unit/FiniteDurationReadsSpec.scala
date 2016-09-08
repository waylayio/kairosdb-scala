package unit

import io.waylay.kairosdb.driver.models.json.Formats._
import org.specs2.mutable.Specification
import play.api.libs.json.{JsError, Json}

import scala.concurrent.duration._

class FiniteDurationReadsSpec extends Specification {
  "Finite duration reads" should {

    "read milliseconds" in {
      val json = Json.parse("""{"unit":"milliseconds","value":10}""")
      json.validate[FiniteDuration].asOpt must beSome(10.millis)
    }

    "read seconds" in {
      val json = Json.parse("""{"unit":"seconds","value":10}""")
      json.validate[FiniteDuration].asOpt must beSome(10.seconds)
    }

    "read minutes" in {
      val json = Json.parse("""{"unit":"minutes","value":10}""")
      json.validate[FiniteDuration].asOpt must beSome(10.minutes)
    }

    "read hours" in {
      val json = Json.parse("""{"unit":"hours","value":10}""")
      json.validate[FiniteDuration].asOpt must beSome(10.hours)
    }

    "read days" in {
      val json = Json.parse("""{"unit":"days","value":10}""")
      json.validate[FiniteDuration].asOpt must beSome(10.days)
    }

    "read weeks" in {
      val json = Json.parse("""{"unit":"weeks","value":10}""")
      json.validate[FiniteDuration].asOpt must beSome((10 * 7).days)
    }

    "fail to read months" in {
      val json = Json.parse("""{"unit":"months","value":10}""")
      json.validate[FiniteDuration] must be equalTo JsError("unit must be one of: milliseconds, seconds, minutes, hours, days, weeks. (months and years are supported by KairosDB but not this driver)")
    }

    "fail to read years" in {
      val json = Json.parse("""{"unit":"years","value":10}""")
      json.validate[FiniteDuration] must be equalTo JsError("unit must be one of: milliseconds, seconds, minutes, hours, days, weeks. (months and years are supported by KairosDB but not this driver)")
    }
  }

}

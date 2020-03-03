package unit

import io.waylay.kairosdb.driver.models.TimeRange
import io.waylay.kairosdb.driver.models.TimeRange.{DAYS, HOURS, MILLISECONDS, MINUTES, MONTHS, SECONDS, WEEKS, YEARS}
import io.waylay.kairosdb.driver.models.json.Formats._
import org.specs2.mutable.Specification
import play.api.libs.json.{JsError, JsSuccess, Json}

class FiniteDurationReadsSpec extends Specification {
  "Finite duration reads" should {

    "read milliseconds" in {
      val json = Json.parse("""{"unit":"milliseconds","value":10}""")
      json.validate[TimeRange].asOpt must beSome(TimeRange(10, MILLISECONDS))
    }

    "read seconds" in {
      val json = Json.parse("""{"unit":"seconds","value":10}""")
      json.validate[TimeRange].asOpt must beSome(TimeRange(10, SECONDS))
    }

    "read minutes" in {
      val json = Json.parse("""{"unit":"minutes","value":10}""")
      json.validate[TimeRange].asOpt must beSome(TimeRange(10, MINUTES))
    }

    "read hours" in {
      val json = Json.parse("""{"unit":"hours","value":10}""")
      json.validate[TimeRange].asOpt must beSome(TimeRange(10, HOURS))
    }

    "read days" in {
      val json = Json.parse("""{"unit":"days","value":10}""")
      json.validate[TimeRange].asOpt must beSome(TimeRange(10, DAYS))
    }

    "read weeks" in {
      val json = Json.parse("""{"unit":"weeks","value":10}""")
      json.validate[TimeRange].asOpt must beSome(TimeRange(10, WEEKS))
    }

    "read months" in {
      val json = Json.parse("""{"unit":"months","value":10}""")
      json.validate[TimeRange].asOpt must beSome(TimeRange(10, MONTHS))
    }

    "read years" in {
      val json = Json.parse("""{"unit":"years","value":10}""")
      json.validate[TimeRange].asOpt must beSome(TimeRange(10, YEARS))
    }

    "fail to read garbage" in {
      val json = Json.parse("""{"unit":"miniyears","value":10}""")
      json.validate[TimeRange] must be equalTo JsError("unit must be one of: milliseconds, seconds, minutes, hours, days, weeks, months or years")
    }

    "read value from a valid long string" in {
      val json = Json.parse("""{"unit":"years","value":"10"}""")
      json.validate[TimeRange] must beEqualTo(JsSuccess(TimeRange(10, YEARS)))
    }

    "failed to read a non-numeric value string" in {
      val json = Json.parse("""{"unit":"years","value":"1sdfsd0"}""")
      json.validate[TimeRange] must beEqualTo(JsError("error.expected.jsnumber"))
    }
  }

}

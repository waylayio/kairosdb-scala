package unit

import io.waylay.kairosdb.driver.models.KairosCompatibleType.{KNumber, KString}
import io.waylay.kairosdb.driver.models.json.Formats.kairosCompatibleTypeReads
import org.specs2.mutable.Specification
import play.api.libs.json._

class KairosCompatibleTypeReadsSpec extends Specification {
  "KairosCompatibleType JSON reads" should {
    "correctly read a JSON number" in {
      val json = JsNumber(55)
      kairosCompatibleTypeReads.reads(json) must be equalTo JsSuccess(KNumber(55))
    }

    "correctly read a JSON string" in {
      val json = JsString("aoeu")
      kairosCompatibleTypeReads.reads(json) must be equalTo JsSuccess(KString("aoeu"))
    }


    "fail on JSON boolean (not supported for now)" in {
      val json = JsBoolean(true)
      kairosCompatibleTypeReads.reads(json) must beAnInstanceOf[JsError]
    }

    "fail on JSON array" in {
      val json = JsArray(Seq(JsString("aoeu"), JsString("snth")))
      kairosCompatibleTypeReads.reads(json) must beAnInstanceOf[JsError]
    }
  }

}

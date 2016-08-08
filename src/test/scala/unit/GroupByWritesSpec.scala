package unit

import io.waylay.kairosdb.driver.models.json.Formats.groupByWrites
import io.waylay.kairosdb.driver.models.GroupBy._
import org.specs2.mutable.Specification
import play.api.libs.json.Json

import scala.concurrent.duration._

class GroupByWritesSpec extends Specification {
  "GroupByTags JSON serialization" should {
    "correctly serialize #1" in {
      Json.toJson(GroupByTags(Seq("tagName1", "tagName2"))) should be equalTo Json.obj(
        "name" -> "tag",
        "tags" -> Seq("tagName1", "tagName2")
      )
    }

    "correctly serialize #2" in {
      Json.toJson(GroupByTags(Seq("mytag"))) should be equalTo Json.obj(
        "name" -> "tag",
        "tags" -> Seq("mytag")
      )
    }
  }

  "GroupByTime JSON serialization" should {
    "correctly serialize #1" in {
      val groupBy = GroupByTime(300.milliseconds, 200)
      Json.toJson(groupBy) should be equalTo Json.obj(
        "name" -> "time",
        "range_size" -> Json.obj("unit" -> "milliseconds", "value" -> "300"),
        "group_count" -> "200"
      )
    }

    "correctly serialize #2" in {
      val groupBy = GroupByTime(1.hour, 168)
      Json.toJson(groupBy) should be equalTo Json.obj(
        "name" -> "time",
        "range_size" -> Json.obj("unit" -> "hours", "value" -> "1"),
        "group_count" -> "168"
      )
    }
  }

  "GroupByValue JSON serialization" should {
    "correctly serialize #1" in {
      Json.toJson(GroupByValue(1000)) should be equalTo Json.obj(
        "name" -> "value",
        "range_size" -> 1000
      )
    }

    "correctly serialize #2" in {
      Json.toJson(GroupByValue(168)) should be equalTo Json.obj(
        "name" -> "value",
        "range_size" -> 168
      )
    }
  }

  "GroupByBins" should {
    "correctly serialize #1" in {
      Json.toJson(GroupByBins(Seq("2", "4", "6", "8"))) should be equalTo Json.obj(
        "name" -> "bin",
        "bins" -> Seq("2", "4", "6", "8")
      )
    }
    "correctly serialize #2" in {
      Json.toJson(GroupByBins(Seq("0", "17", "168"))) should be equalTo Json.obj(
        "name" -> "bin",
        "bins" -> Seq("0", "17", "168")
      )
    }
  }
}

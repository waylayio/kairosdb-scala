package unit

import java.time.Instant
import java.util.concurrent.TimeUnit

import io.waylay.kairosdb.driver.models.json.Formats._
import io.waylay.kairosdb.driver.models.Aggregator.Trim.{TrimBoth, TrimFirst, TrimLast}
import io.waylay.kairosdb.driver.models.{MetricName, Tag}
import io.waylay.kairosdb.driver.Implicits.finiteDuration2timeRange
import io.waylay.kairosdb.driver.models.Aggregator._
import io.waylay.kairosdb.driver.models.KairosCompatibleType.{KNumber, KString}
import io.waylay.kairosdb.driver.models.RangeAggregator.Align._
import io.waylay.kairosdb.driver.models.TimeSpan.AbsoluteStartTime
import org.specs2.mutable.Specification
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.collection.immutable.Seq

class AggregatorWritesSpec extends Specification {
  "Average aggregator" should {
    "correctly serialize minimal example" in {
      val aggregator = Average(1.minutes)
      Json.toJson(aggregator) should be equalTo Json.obj(
        "name" -> "avg",
        "sampling" -> Json.obj(
          "value" -> "1", // TODO example uses 1 (int, not string). Unclear if KairosDB uses a liberal parser or if they parse RelativeTimeValue inconsistently
                          //      get back to this once this ticket is resolved: https://github.com/kairosdb/kairosdb/issues/311
          "unit" -> "minutes"
        )
      )
    }

    "correctly serialize with a different sampling rate" in {
      val aggregator = Average(9.days)
      Json.toJson(aggregator) should be equalTo Json.obj(
        "name" -> "avg",
        "sampling" -> Json.obj(
          "value" -> "9",
          "unit" -> "days"
        )
      )
    }

    "correctly serialize with sampling aligning" in {
      val aggregator = Average(1.minutes, align = Some(AlignSampling))
      Json.toJson(aggregator) should be equalTo Json.obj(
        "name" -> "avg",
        "align_sampling" -> true,
        "sampling" -> Json.obj(
          "value" -> "1",
          "unit" -> "minutes"
        )
      )
    }

    "correctly serialize with start time aligning" in {
      val aggregator = Average(1.minute, align = Some(AlignStartTime))
      Json.toJson(aggregator) should be equalTo Json.obj(
        "name" -> "avg",
        "align_start_time" -> true,
        "align_sampling"->false,
        "sampling" -> Json.obj(
          "value" -> "1",
          "unit" -> "minutes"
        )
      )
    }

    "correctly serialize with a start time" in {
      val aggregator = Average(1.minute, startTime = Some(AbsoluteStartTime(Instant.ofEpochMilli(1469778770L))))
      Json.toJson(aggregator) should be equalTo Json.obj(
        "name" -> "avg",
        "sampling" -> Json.obj(
          "value" -> "1",
          "unit" -> "minutes"
        ),
        "start_time" -> 1469778770L
      )
    }

    "correctly serialize with a time zone" in {
      val aggregator = Average(1.minute, timeZone = Some("Europe/Brussels"))
      Json.toJson(aggregator) should be equalTo Json.obj(
        "name" -> "avg",
        "sampling" -> Json.obj(
          "value" -> "1",
          "unit" -> "minutes"
        ),
        "time_zone" -> "Europe/Brussels"
      )
    }

    "correctly serialize full example" in {
      val aggregator = Average(3.days, Some(AbsoluteStartTime(Instant.ofEpochMilli(1469778777L))), Some(AlignSampling), Some("Africa/Banjul"))
      Json.toJson(aggregator) should be equalTo Json.obj(
        "name" -> "avg",
        "sampling" -> Json.obj(
          "value" -> "3",
          "unit" -> "days"
        ),
        "start_time" -> 1469778777L,
        "align_sampling" -> true,
        "time_zone" -> "Africa/Banjul"
      )
    }
  }

  "Standard deviation aggregator" should {
    "correcly serialize" in {
      val aggregator = StandardDeviation(3.days, Some(AbsoluteStartTime(Instant.ofEpochMilli(1469778777L))), Some(AlignSampling), Some("Africa/Banjul"))
      Json.toJson(aggregator) should be equalTo Json.obj(
        "name" -> "dev",
        "sampling" -> Json.obj(
          "value" -> "3",
          "unit" -> "days"
        ),
        "start_time" -> 1469778777L,
        "align_sampling" -> true,
        "time_zone" -> "Africa/Banjul"
      )
    }
  }

  "Count aggregator" should {
    "correcly serialize" in {
      val aggregator = Count(3.days, Some(AbsoluteStartTime(Instant.ofEpochMilli(1469778777L))), Some(AlignSampling), Some("Africa/Banjul"))
      Json.toJson(aggregator) should be equalTo Json.obj(
        "name" -> "count",
        "sampling" -> Json.obj(
          "value" -> "3",
          "unit" -> "days"
        ),
        "start_time" -> 1469778777L,
        "align_sampling" -> true,
        "time_zone" -> "Africa/Banjul"
      )
    }
  }

  "First aggregator" should {
    "correcly serialize" in {
      val aggregator = First(3.days, Some(AbsoluteStartTime(Instant.ofEpochMilli(1469778777L))), Some(AlignSampling), Some("Africa/Banjul"))
      Json.toJson(aggregator) should be equalTo Json.obj(
        "name" -> "first",
        "sampling" -> Json.obj(
          "value" -> "3",
          "unit" -> "days"
        ),
        "start_time" -> 1469778777L,
        "align_sampling" -> true,
        "time_zone" -> "Africa/Banjul"
      )
    }
  }

  "Gaps aggregator" should {
    "correcly serialize" in {
      val aggregator = Gaps(3.days, Some(AbsoluteStartTime(Instant.ofEpochMilli(1469778777L))), Some(AlignSampling), Some("Africa/Banjul"))
      Json.toJson(aggregator) should be equalTo Json.obj(
        "name" -> "gaps",
        "sampling" -> Json.obj(
          "value" -> "3",
          "unit" -> "days"
        ),
        "start_time" -> 1469778777L,
        "align_sampling" -> true,
        "time_zone" -> "Africa/Banjul"
      )
    }
  }

  "Last aggregator" should {
    "correcly serialize" in {
      val aggregator = Last(3.days, Some(AbsoluteStartTime(Instant.ofEpochMilli(1469778777L))), Some(AlignSampling), Some("Africa/Banjul"))
      Json.toJson(aggregator) should be equalTo Json.obj(
        "name" -> "last",
        "sampling" -> Json.obj(
          "value" -> "3",
          "unit" -> "days"
        ),
        "start_time" -> 1469778777L,
        "align_sampling" -> true,
        "time_zone" -> "Africa/Banjul"
      )
    }
  }

  "Least squares aggregator" should {
    "correcly serialize" in {
      val aggregator = LeastSquares(3.days, Some(AbsoluteStartTime(Instant.ofEpochMilli(1469778777L))), Some(AlignSampling), Some("Africa/Banjul"))
      Json.toJson(aggregator) should be equalTo Json.obj(
        "name" -> "least_squares",
        "sampling" -> Json.obj(
          "value" -> "3",
          "unit" -> "days"
        ),
        "start_time" -> 1469778777L,
        "align_sampling" -> true,
        "time_zone" -> "Africa/Banjul"
      )
    }
  }

  "Max aggregator" should {
    "correcly serialize" in {
      val aggregator = Max(3.days, Some(AbsoluteStartTime(Instant.ofEpochMilli(1469778777L))), Some(AlignSampling), Some("Africa/Banjul"))
      Json.toJson(aggregator) should be equalTo Json.obj(
        "name" -> "max",
        "sampling" -> Json.obj(
          "value" -> "3",
          "unit" -> "days"
        ),
        "start_time" -> 1469778777L,
        "align_sampling" -> true,
        "time_zone" -> "Africa/Banjul"
      )
    }
  }

  "Min aggregator" should {
    "correcly serialize" in {
      val aggregator = Min(3.days, Some(AbsoluteStartTime(Instant.ofEpochMilli(1469778777L))), Some(AlignSampling), Some("Africa/Banjul"))
      Json.toJson(aggregator) should be equalTo Json.obj(
        "name" -> "min",
        "sampling" -> Json.obj(
          "value" -> "3",
          "unit" -> "days"
        ),
        "start_time" -> 1469778777L,
        "align_sampling" -> true,
        "time_zone" -> "Africa/Banjul"
      )
    }
  }

  "Percentile aggregator" should {
    "correcly serialize 0.7 percentile" in {
      val aggregator = Percentile(0.7, 3.days, Some(AbsoluteStartTime(Instant.ofEpochMilli(1469778777L))), Some(AlignSampling), Some("Africa/Banjul"))
      Json.toJson(aggregator) should be equalTo Json.obj(
        "name" -> "percentile",
        "percentile" -> 0.7,
        "sampling" -> Json.obj(
          "value" -> "3",
          "unit" -> "days"
        ),
        "start_time" -> 1469778777L,
        "align_sampling" -> true,
        "time_zone" -> "Africa/Banjul"
      )
    }

    "correcly serialize 1.0 percentile" in {
      val aggregator = Percentile(1.0, 3.days, Some(AbsoluteStartTime(Instant.ofEpochMilli(1469778777L))), Some(AlignSampling), Some("Africa/Banjul"))
      Json.toJson(aggregator) should be equalTo Json.obj(
        "name" -> "percentile",
        "percentile" -> 1.0,
        "sampling" -> Json.obj(
          "value" -> "3",
          "unit" -> "days"
        ),
        "start_time" -> 1469778777L,
        "align_sampling" -> true,
        "time_zone" -> "Africa/Banjul"
      )
    }

    "correcly serialize 1.0 percentile when aligning start time" in {
      val aggregator = Percentile(1.0, 3.days, Some(AbsoluteStartTime(Instant.ofEpochMilli(1469778777L))), Some(AlignStartTime), Some("Africa/Banjul"))
      Json.toJson(aggregator) should be equalTo Json.obj(
        "name" -> "percentile",
        "percentile" -> 1.0,
        "sampling" -> Json.obj(
          "value" -> "3",
          "unit" -> "days"
        ),
        "start_time" -> 1469778777L,
        "align_start_time" -> true,
        "align_sampling"->false,
        "time_zone" -> "Africa/Banjul"
      )
    }
  }

  "Sum aggregator" should {
    "correcly serialize" in {
      val aggregator = Sum(3.days, Some(AbsoluteStartTime(Instant.ofEpochMilli(1469778777L))), Some(AlignSampling), Some("Africa/Banjul"))
      Json.toJson(aggregator) should be equalTo Json.obj(
        "name" -> "sum",
        "sampling" -> Json.obj(
          "value" -> "3",
          "unit" -> "days"
        ),
        "start_time" -> 1469778777L,
        "align_sampling" -> true,
        "time_zone" -> "Africa/Banjul"
      )
    }
  }

  "Diff aggregator" should {
    "correctly serialize" in {
      Json.toJson(Diff()) should be equalTo Json.obj(
        "name" -> "diff"
      )
    }
  }

  "Divide aggregator" should {
    "correctly serialize divisor 7" in {
      Json.toJson(Divide(7)) should be equalTo Json.obj(
        "name" -> "div",
        "divisor" -> 7
      )
    }

    "correctly serialize divisor 2.5" in {
      Json.toJson(Divide(2.5)) should be equalTo Json.obj(
        "name" -> "div",
        "divisor" -> 2.5
      )
    }
  }

  "Rate aggregator" should {
    "correctly serialize" in {
      Json.toJson(Rate(TimeUnit.SECONDS, 2.days, None)) should be equalTo Json.obj(
        "name" -> "rate",
        "unit" -> "seconds",
        "sampling" -> Json.obj(
          "unit" -> "days",
          "value" -> "2"
        )
      )
    }

    "throw exception when trying to use microseconds" in {
      Json.toJson(Rate(TimeUnit.MICROSECONDS, 2.days, None)) should throwAn[IllegalArgumentException]
    }

    "throw exception when trying to use nanoseconds" in {
      Json.toJson(Rate(TimeUnit.NANOSECONDS, 2.days, None)) should throwAn[IllegalArgumentException]
    }

    "correctly serialize with time zone" in {
      Json.toJson(Rate(TimeUnit.SECONDS, 3.days, Some("America/Chihuahua"))) should be equalTo Json.obj(
        "name" -> "rate",
        "unit" -> "seconds",
        "sampling" -> Json.obj(
          "unit" -> "days",
          "value" -> "3"
        ),
        "time_zone" -> "America/Chihuahua"
      )
    }
  }

  "Sampler aggregator" should {
    "correctly serialize" in {
      Json.toJson(Sampler(TimeUnit.HOURS, None)) should be equalTo Json.obj(
        "name" -> "sampler",
        "unit" -> "hours"
      )
    }

    "correctly serialize with time zone" in {
      Json.toJson(Sampler(TimeUnit.MINUTES, Some("Europe/Copenhagen"))) should be equalTo Json.obj(
        "name" -> "sampler",
        "unit" -> "minutes",
        "time_zone" -> "Europe/Copenhagen"
      )
    }
  }

  "Scale aggregator" should {
    "correctly serialize factor 5" in {
      Json.toJson(Scale(5)) should be equalTo Json.obj(
        "name" -> "scale",
        "factor" -> 5
      )
    }

    "correctly serialize factor 2.5" in {
      Json.toJson(Scale(2.5)) should be equalTo Json.obj(
        "name" -> "scale",
        "factor" -> 2.5
      )
    }
  }

  "Trim aggregator" should {
    "correctly serialize trim last" in {
      Json.toJson(Trim(TrimLast)) should be equalTo Json.obj(
        "name" -> "trim",
        "trim" -> "LAST"
      )
    }

    "correctly serialize trim first" in {
      Json.toJson(Trim(TrimBoth)) should be equalTo Json.obj(
        "name" -> "trim",
        "trim" -> "BOTH"
      )
    }

    "correctly serialize trim both" in {
      Json.toJson(Trim(TrimFirst)) should be equalTo Json.obj(
        "name" -> "trim",
        "trim" -> "FIRST"
      )
    }
  }

  "Save as aggregator" should {
    "correctly serialize minimal example" in {
      Json.toJson(SaveAs(MetricName("new-metric-name"), Seq(), 0.seconds)) should be equalTo Json.obj(
        "name" -> "save_as",
        "metric_name" -> "new-metric-name",
        "tags" -> Json.obj(),
        "ttl" -> 0
      )
    }

    "correctly serialize" in {
      Json.toJson(SaveAs(MetricName("new-metric-name"), Seq(Tag("newname", "newvalue"), Tag("second", "stuff")), 0.seconds)) should be equalTo Json.obj(
        "name" -> "save_as",
        "metric_name" -> "new-metric-name",
        "tags" -> Json.obj("newname" -> "newvalue", "second" -> "stuff"),
        "ttl" -> 0
      )
    }

    "correctly serialize with ttl 500" in {
      Json.toJson(SaveAs(MetricName("new-metric-name"), Seq(Tag("newname", "newvalue"), Tag("second", "stuff")), 500.seconds)) should be equalTo Json.obj(
        "name" -> "save_as",
        "metric_name" -> "new-metric-name",
        "tags" -> Json.obj("newname" -> "newvalue", "second" -> "stuff"),
        "ttl" -> 500
      )
    }

    "correctly serialize with ttl 4 days" in {
      Json.toJson(SaveAs(MetricName("new-metric-name"), Seq(Tag("newname", "newvalue"), Tag("second", "stuff")), 4.days)) should be equalTo Json.obj(
        "name" -> "save_as",
        "metric_name" -> "new-metric-name",
        "tags" -> Json.obj("newname" -> "newvalue", "second" -> "stuff"),
        "ttl" -> 345600
      )
    }

    "correctly serialize with ttl 2 millis" in {
      Json.toJson(SaveAs(MetricName("new-metric-name"), Seq(Tag("newname", "newvalue"), Tag("second", "stuff")), 4.milliseconds)) should be equalTo Json.obj(
        "name" -> "save_as",
        "metric_name" -> "new-metric-name",
        "tags" -> Json.obj("newname" -> "newvalue", "second" -> "stuff"),
        "ttl" -> 1
      )
    }
  }

  "Filter aggregator" should {
    "correctly serialize with number threshold" in {
      Json.toJson(Filter("lt", KNumber(3.7997))) should be equalTo Json.obj(
        "name" -> "filter",
        "filter_op" -> "lt",
        "threshold" -> 3.7997
      )
    }
    "correctly serialize with string threshold" in {
      Json.toJson(Filter("eq", KString("3.7997"))) should be equalTo Json.obj(
        "name" -> "filter",
        "filter_op" -> "eq",
        "threshold" -> "3.7997"
      )
    }
  }
}

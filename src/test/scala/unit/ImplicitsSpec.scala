package unit

import java.time.Instant

import io.waylay.kairosdb.driver.models.KairosCompatibleType._
import io.waylay.kairosdb.driver.models.KairosQuery.QueryTag
import io.waylay.kairosdb.driver.models._
import io.waylay.kairosdb.driver.models.TimeSpan.{AbsoluteEndTime, AbsoluteStartTime, RelativeEndTime, RelativeStartTime}
import org.specs2.mutable.Specification
import org.specs2.matcher.ResultMatchers

import scala.concurrent.duration._

class ImplicitsSpec extends Specification with ResultMatchers {
  "The implicits" should {
    "correctly convert types implicitly" in {
      import io.waylay.kairosdb.driver.Implicits._

      val kString: KString = "aoeu"
      kString should be equalTo KString("aoeu")

      val kNumber: KNumber = 555
      kNumber should be equalTo KNumber(555)

      val kLong: KNumber = 555L
      kLong should be equalTo KNumber(555)

      val metricName: MetricName = "metric.name"
      metricName should be equalTo MetricName("metric.name")

      val doubleKNum: KNumber = 3.141565d
      doubleKNum should be equalTo KNumber(BigDecimal(3.141565))

      val floatKNum: KNumber = 3.14f
      floatKNum should be equalTo KNumber(BigDecimal(3.14f.toDouble))

      val relStart: RelativeStartTime = 5.seconds
      relStart should be equalTo RelativeStartTime(5.seconds)

      val relEnd: RelativeEndTime = 5.hours
      relEnd should be equalTo RelativeEndTime(5.hours)

      val absStart: AbsoluteStartTime = Instant.ofEpochMilli(50000L)
      absStart should be equalTo AbsoluteStartTime(Instant.ofEpochMilli(50000L))

      val absEnd: AbsoluteEndTime = Instant.ofEpochMilli(50000L)
      absEnd should be equalTo AbsoluteEndTime(Instant.ofEpochMilli(50000L))

      val relTimeSpan: TimeSpan = 4.days
      relTimeSpan should be equalTo TimeSpan(RelativeStartTime(4.days))

      val insTimeSpan: TimeSpan = Instant.ofEpochMilli(1111111L)
      insTimeSpan should be equalTo TimeSpan(AbsoluteStartTime(Instant.ofEpochMilli(1111111L)))

      val insTupleTimeSpan: TimeSpan = Instant.ofEpochMilli(111111L) -> Instant.ofEpochMilli(211111L)
      insTupleTimeSpan should be equalTo TimeSpan(AbsoluteStartTime(Instant.ofEpochMilli(111111L)), Some(Instant.ofEpochMilli(211111L)))

      val relTupleTimeSpan: TimeSpan = 2.hours -> 2.minutes
      relTupleTimeSpan should be equalTo TimeSpan(RelativeStartTime(2.hours), Some(RelativeEndTime(2.minutes)))

      val relStartTimeSpan: TimeSpan = RelativeStartTime(4.hours)
      relStartTimeSpan should be equalTo TimeSpan(RelativeStartTime(4.hours))

      val absStartTimeSpan: TimeSpan = AbsoluteStartTime(Instant.ofEpochMilli(111111L))
      absStartTimeSpan should be equalTo TimeSpan(AbsoluteStartTime(Instant.ofEpochMilli(111111L)))

      val tagSeq: Seq[Tag] = Tag("aoeu", "snth")
      tagSeq should be equalTo Seq(Tag("aoeu", "snth"))

      val queryTagSeq: Seq[QueryTag] = QueryTag("key", Seq("value1", "value2"))
      queryTagSeq should be equalTo Seq(QueryTag("key", Seq("value1", "value2")))

      val querySeq: Seq[Query] = Query("aoeu")
      querySeq should be equalTo Seq(Query("aoeu"))

      val qmSeq: Seq[QueryMetrics] = QueryMetrics(Seq(Query("aoeu")), TimeSpan(AbsoluteStartTime(Instant.ofEpochMilli(111111L))))
      qmSeq should be equalTo QueryMetrics(Seq(Query("aoeu")), TimeSpan(AbsoluteStartTime(Instant.ofEpochMilli(111111L))))

      val relStartFin: RelativeStartTime = 5.minutes.ago.startTime
      relStartFin should be equalTo RelativeStartTime(5.minutes)

      val relEndFin: RelativeEndTime = 5.minutes.ago.endTime
      relEndFin should be equalTo RelativeEndTime(5.minutes)


    }
  }
}

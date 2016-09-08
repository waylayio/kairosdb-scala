package io.waylay.kairosdb.driver

import java.time.Instant

import io.waylay.kairosdb.driver.models.KairosCompatibleType.{KNumber, KString}
import io.waylay.kairosdb.driver.models.KairosQuery.QueryTag
import io.waylay.kairosdb.driver.models._
import io.waylay.kairosdb.driver.models.TimeSpan.{AbsoluteEndTime, AbsoluteStartTime, RelativeEndTime, RelativeStartTime}

import scala.concurrent.duration.FiniteDuration

object Implicits {
  implicit def string2metricName(str: String): MetricName = MetricName(str)

  implicit def string2kstring(str: String): KString = KString(str)

  implicit def int2knumber(int: Int): KNumber = KNumber(int)

  implicit def long2knumber(long: Long): KNumber = KNumber(long)

  implicit def double2knumber(double: Double): KNumber = KNumber(double)

  implicit def float2knumber(float: Float): KNumber = KNumber(float.toDouble)

  implicit def finiteDuration2relativeStartTime(fin: FiniteDuration): RelativeStartTime = RelativeStartTime(fin)

  implicit def finiteDuration2timeSpan(fin: FiniteDuration): TimeSpan = TimeSpan(RelativeStartTime(fin))

  implicit def finiteDuration2relativeEndTime(fin: FiniteDuration): RelativeEndTime = RelativeEndTime(fin)

  implicit def instant2absoluteStartTime(in: Instant): AbsoluteStartTime = AbsoluteStartTime(in)

  implicit def instant2timeSpan(in: Instant): TimeSpan = TimeSpan(AbsoluteStartTime(in))

  implicit def instant2absoluteEndTime(in: Instant): AbsoluteEndTime = AbsoluteEndTime(in)

  implicit def instantTuple2timespan(span: (Instant, Instant)): TimeSpan = {
    val (start, end) = span
    TimeSpan(AbsoluteStartTime(start), Some(AbsoluteEndTime(end)))
  }

  implicit def finiteDurationTuple2timespan(span: (FiniteDuration, FiniteDuration)): TimeSpan = {
    val (start, end) = span
    TimeSpan(RelativeStartTime(start), Some(RelativeEndTime(end)))
  }

  implicit def relativeStartTime2timeSpan(start: RelativeStartTime): TimeSpan = TimeSpan(start)

  implicit def absoluteStartTime2timeSpan(start: AbsoluteStartTime): TimeSpan = TimeSpan(start)

  implicit def tag2seqTag(tag: Tag): Seq[Tag] = Seq(tag)

  implicit def queryTag2seqQueryTag(tag: QueryTag): Seq[QueryTag] = Seq(tag)

  implicit def query2seqQuery(query: Query): Seq[Query] = Seq(query)

  implicit def queryMetrics2seqQueryMetrics(qm: QueryMetrics): Seq[QueryMetrics] = Seq(qm)

  implicit class PimpedFiniteDuration(fin: FiniteDuration) {
    case class RelativeTime(dur: FiniteDuration) {
      def startTime: RelativeStartTime = RelativeStartTime(dur)
      def endTime: RelativeEndTime = RelativeEndTime(dur)
    }

    def ago = RelativeTime(fin)
  }
}

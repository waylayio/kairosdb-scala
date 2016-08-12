package unit

import java.time.Instant

import io.waylay.kairosdb.driver.models.TimeSpan
import io.waylay.kairosdb.driver.models.TimeSpan.{AbsoluteEndTime, AbsoluteStartTime, RelativeEndTime, RelativeStartTime}
import org.specs2.matcher.{FutureMatchers, ResultMatchers}
import org.specs2.mutable.Specification

import scala.concurrent.duration._

class TimeSpec extends Specification with FutureMatchers with ResultMatchers {
  "Relative start time" should {
    "toMillis with reference point should work" in {
      val relativeStartTime = RelativeStartTime(40.seconds)
      relativeStartTime.toMillis(Instant.ofEpochSecond(1471003248L)) should be equalTo 1471003208000L
    }
  }

  "Relative end time" should {
    "toMillis with reference point should work" in {
      val relativeEndTime = RelativeEndTime(40.seconds)
      relativeEndTime.toMillis(Instant.ofEpochSecond(1471003248L)) should be equalTo 1471003208000L
    }
  }

  "Absolute start time" should {
    "toMillis should return the initial value" in {
      AbsoluteStartTime(Instant.ofEpochSecond(1471003248L)).toMillis should be equalTo 1471003248000L
    }
  }

  "Absolute end time" should {
    "toMillis should return the initial value" in {
      AbsoluteEndTime(Instant.ofEpochSecond(1471103248L)).toMillis should be equalTo 1471103248000L
    }
  }

  "Timespan#toMillis" should {
    "work" in {
      val ts = TimeSpan(AbsoluteStartTime(Instant.ofEpochSecond(1471003248L)), Some(AbsoluteEndTime(Instant.ofEpochSecond(1471007154L))))
      ts.toMillis should be equalTo 3906000L
    }
  }
}

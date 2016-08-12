package io.waylay.kairosdb.driver.models

import java.time.Instant

import io.waylay.kairosdb.driver.models.TimeSpan.{EndTime, StartTime}

import scala.concurrent.duration._

/**
  * @param endTime If end time is None, the current date and time is assumed
  */
case class TimeSpan(startTime: StartTime, endTime: Option[EndTime] = None) {
  def toMillis: Long = (endTime map(_.toMillis) getOrElse Instant.now.toEpochMilli) - startTime.toMillis
}

object TimeSpan {
  sealed trait TimePoint {
    val fieldName: String
  }
  sealed trait StartTime extends TimePoint {
    def toMillis: Long
  }

  sealed trait EndTime extends TimePoint {
    def toMillis: Long
  }


  case class AbsoluteStartTime(startTime: Instant) extends StartTime {
    override val fieldName = "start_absolute"
    override def toMillis = startTime.toEpochMilli
  }
  case class RelativeStartTime(howLongAgo: FiniteDuration) extends StartTime {
    override val fieldName = "start_relative"
    override def toMillis: Long = toMillis(Instant.now)

    def toMillis(reference: Instant) = reference.minusMillis(howLongAgo.toMillis).toEpochMilli
  }


  case class AbsoluteEndTime(endTime: Instant) extends EndTime {
    override val fieldName = "end_absolute"
    override def toMillis = endTime.toEpochMilli
  }
  case class RelativeEndTime(howLongAgo: FiniteDuration) extends EndTime {
    override val fieldName = "end_relative"
    override def toMillis = toMillis(Instant.now)

    def toMillis(reference: Instant) = reference.minusMillis(howLongAgo.toMillis).toEpochMilli
  }
}

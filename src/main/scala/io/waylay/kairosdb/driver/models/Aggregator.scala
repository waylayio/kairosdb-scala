package io.waylay.kairosdb.driver.models

import io.waylay.kairosdb.driver.models.Aggregator.Trim.TrimWhat
import io.waylay.kairosdb.driver.models.RangeAggregator.Align
import io.waylay.kairosdb.driver.models.TimeSpan.AbsoluteStartTime

import scala.concurrent.duration.{FiniteDuration, TimeUnit}

sealed trait Aggregator {
  val name: String
}

object RangeAggregator {
  sealed trait Align

  object Align {

    /** When set, the time for the aggregated data point for each range will fall on the start of the range instead
      * of being the value for the first data point within that range. */
    case class AlignStartTime() extends Align

    /** Setting this to will cause the aggregation range to be aligned based on the sampling size. For example if
      * your sample size is either milliseconds, seconds, minutes or hours then the start of the range will always be at
      * the top of the hour. The effect of setting this to true is that your data will take the same shape when graphed
      * as you refresh the data */
    case class AlignSampling() extends Align

  }
}

/** Many of the aggregators inherit from the range aggregator */
trait RangeAggregator extends Aggregator {
  /** Sampling is the length of the interval on which to aggregate data */
  val sampling: FiniteDuration
  val align: Option[Align]
  /** Start time to calculate the ranges from. Typically this is the start of the query. */
  val startTime: Option[AbsoluteStartTime]
  /** Time zone to use when doing time based calculations. */
  val timeZone: Option[String]
}

object Aggregator {

  case class Average(sampling: FiniteDuration, startTime: Option[AbsoluteStartTime] = None, align: Option[Align] = None, timeZone: Option[String] = None)
    extends RangeAggregator {
    override val name = "avg"
  }

  /** Computes standard deviation */
  case class StandardDeviation(sampling: FiniteDuration, startTime: Option[AbsoluteStartTime] = None, align: Option[Align] = None, timeZone: Option[String] = None)
    extends RangeAggregator {
    override val name = "dev"
  }

  /** Counts the number of data points */
  case class Count(sampling: FiniteDuration, startTime: Option[AbsoluteStartTime] = None, align: Option[Align] = None, timeZone: Option[String] = None)
    extends RangeAggregator {
    override val name = "count"
  }

  /** Returns the first data point for the interval */
  case class First(sampling: FiniteDuration, startTime: Option[AbsoluteStartTime] = None, align: Option[Align] = None, timeZone: Option[String] = None)
    extends RangeAggregator {
    override val name = "first"
  }

  /** Marks gaps in data according to sampling rate with a null data point */
  case class Gaps(sampling: FiniteDuration, startTime: Option[AbsoluteStartTime] = None, align: Option[Align] = None, timeZone: Option[String] = None)
    extends RangeAggregator {
    override val name = "gaps"
  }

  /** Returns the last data point for the interval */
  case class Last(sampling: FiniteDuration, startTime: Option[AbsoluteStartTime] = None, align: Option[Align] = None, timeZone: Option[String] = None)
    extends RangeAggregator {
    override val name = "last"
  }

  /* Returns two points for the range which represent the best fit line through the set of points */
  case class LeastSquares(sampling: FiniteDuration, startTime: Option[AbsoluteStartTime] = None, align: Option[Align] = None, timeZone: Option[String] = None)
    extends RangeAggregator {
    override val name = "least_squares"
  }

  case class Max(sampling: FiniteDuration, startTime: Option[AbsoluteStartTime] = None, align: Option[Align] = None, timeZone: Option[String] = None)
    extends RangeAggregator {
    override val name = "max"
  }

  case class Min(sampling: FiniteDuration, startTime: Option[AbsoluteStartTime] = None, align: Option[Align] = None, timeZone: Option[String] = None)
    extends RangeAggregator {
    override val name = "min"
  }

  /** Finds the percentile of the data range. Calculates a probability distribution and returns the specified percentile
    * for the distribution.
    *
    * @param percentile Defined as 0 < percentile <= 1 where .5 is 50% and 1 is 100%
    */
  case class Percentile(percentile: Double, sampling: FiniteDuration, startTime: Option[AbsoluteStartTime] = None, align: Option[Align] = None, timeZone: Option[String] = None)
    extends RangeAggregator {
    override val name = "percentile"
  }

  /** Sums all value */
  case class Sum(sampling: FiniteDuration, startTime: Option[AbsoluteStartTime] = None, align: Option[Align] = None, timeZone: Option[String] = None)
    extends RangeAggregator {
    override val name = "sum"
  }

  /** Computes the difference between successive data points */
  case class Diff() extends Aggregator {
    override val name = "diff"
  }

  /** Returns each data point divided by a divisor. Requires a “divisor” property which is the value that all data points will be divided by. */
  case class Divide(divisor: Double) extends Aggregator {
    override val name = "div"
  }

  /** Returns the rate of change between a pair of data points. Requires a “unit” property which is the sampling duration (ie rate in seconds, milliseconds, minutes, etc...). */
  case class Rate(unit: TimeUnit, sampling: FiniteDuration, timezone: Option[String]) extends Aggregator {
    override val name = "rate"
  }

  /** Computes the sampling rate of change for the data points
    *
    * @param unit Sets the sampling unit. If you set the unit to SECONDS then the sampling rate is over one second
    */
  case class Sampler(unit: TimeUnit, timezone: Option[String]) extends Aggregator {
    override val name = "sampler"
  }

  /** Scales each data point by a factor */
  case class Scale(factor: Double) extends Aggregator {
    override val name = "scale"
  }

  /** Trims off the first, last or both data points for the interval.
    * Useful in conjunction with the SaveAs aggregator to remove partial intervals. */
  case class Trim(trimWhat: TrimWhat) extends Aggregator {
    override val name = "trim"
  }

  object Trim {

    sealed trait TrimWhat {
      val value: String
    }

    case class TrimFirst() extends TrimWhat {
      override val value = "FIRST"
    }

    case class TrimLast() extends TrimWhat {
      override val value = "LAST"
    }

    case class TrimBoth() extends TrimWhat {
      override val value = "BOTH"
    }

  }

  /** Saves the result to another metric. Any data point with a unique tag value will also have that tag set.
    * So if a data point is returned with tags `{"dc":["DC1"],"host":["hostA", "hostB"]}` only the `dc` tag will be set
    * when saved. If you do a group by query the group by tags are saved.
    *
    * @param metricName Metric name to save the results to.
    * @param tags       Additional tags to set on the metrics {"tag1":"value1","tag2":"value2"}
    * @param ttl        Sets the ttl on the newly saved metrics
    */
  case class SaveAs(metricName: MetricName, tags: Seq[Tag], ttl: FiniteDuration) extends Aggregator { // TODO ttl should be Option?
    override val name = "save_as"
  }
}

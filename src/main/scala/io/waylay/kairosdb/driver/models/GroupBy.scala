package io.waylay.kairosdb.driver.models

sealed trait GroupBy {
  val name: String
}

object GroupBy {

  /** You can group results by specifying one or more tag names. For example, if you have a customer tag, grouping by
    * customer would create a resulting object for each customer.
    *
    * Multiple tag names can be used to further group the data points.
    */
  case class GroupByTags(tags: Seq[String]) extends GroupBy {
    override val name = "tag"
  }

  /** The time grouper groups results by time ranges. For example, you could group data by day of week.
    *
    * Note that the grouper calculates ranges based on the start time of the query. So if you wanted to group by day
    * of week and wanted the first group to be Sunday, then you need to set the query’s start time to be on Sunday.
    *
    * @param rangeSize A value and a unit. For example, 1 day would group by day of the week (Sunday - Saturday).
    * @param groupCount The number of groups. This would typically be 7 to group by day of week. But you could set this
    *                   to 14 to group by fortnight.
    */
  case class GroupByTime(rangeSize: TimeRange, groupCount: Int) extends GroupBy {
    override val name = "time"
  }

  /** The value grouper groups by data point values. Values are placed into groups based on a range size. For example,
    * if the range size is 10, then values between 0-9 are placed in the first group, values between 10-19 into the
    * second group, and so forth.
    */
  case class GroupByValue(rangeSize: Int) extends GroupBy {
    override val name = "value"
  }

  /** Each object of the response JSON contains the group_by information you specified in the query as well as a group
    * object. The group object contains the group number starting with a group number of 0. For example, for
    * Seq("2", "4", "6", "8"), the first group (bin number 0) contains data points whose values are between 0 and 2.
    * The second group (bin number 1) contains data points whose values are between 2 and 4, etc.
    */
  case class GroupByBins(bins: Seq[String]) extends GroupBy {
    override val name = "bin"
  }

  /* Unclear if you can also compose queries with this. In any case, it's returned from query responses. */
  case class GroupByType(typeName: String) extends GroupBy {
    override val name = "type"
  }
}

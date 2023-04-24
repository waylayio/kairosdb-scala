package io.waylay.kairosdb.driver.models.json

import java.time.Instant
import java.util.concurrent.TimeUnit

import io.waylay.kairosdb.driver.models.Aggregator._
import io.waylay.kairosdb.driver.models.GroupBy._
import io.waylay.kairosdb.driver.models.KairosCompatibleType.{KNull, KNumber, KString}
import io.waylay.kairosdb.driver.models.KairosQuery.{Order, QueryTag}
import io.waylay.kairosdb.driver.models.QueryMetricTagsResponse.{TagQueryResponse, TagsResponse, TagsResult}
import io.waylay.kairosdb.driver.models.QueryResponse.{Response, ResponseQuery, Result, TagResult}
import io.waylay.kairosdb.driver.models.RangeAggregator.Align.{AlignSampling, AlignStartTime}
import io.waylay.kairosdb.driver.models.TimeRange.{DAYS, HOURS, KairosTimeUnit, MILLISECONDS, MINUTES, SECONDS, _}
import io.waylay.kairosdb.driver.models.TimeSpan._
import io.waylay.kairosdb.driver.models.{Aggregator, KairosCompatibleType, RangeAggregator, _}
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.collection.immutable.Seq
import scala.util.{Failure, Success, Try}

object Formats {

  implicit val datapointWrites: Writes[DataPoint] = new Writes[DataPoint] {

    implicit val datapointWithTimeStampWrites =
      new Writes[(Instant, KairosCompatibleType)] {
        override def writes(point: (Instant, KairosCompatibleType)): JsValue = {
          val (time, value) = point
          Json.arr(time.toEpochMilli, value)
        }
      }

    override def writes(datapoint: DataPoint): JsValue = {
      val tags = JsObject(
        datapoint.tags.map(tag => (tag.name, JsString(tag.value))).toMap
      )

      val ttl =
        datapoint.ttl.fold(Json.obj())(x => Json.obj("ttl" -> timeRange2ttl(x)))

      val value: JsObject = datapoint match {
        case dp: DataPointWithSingleValue =>
          Json.obj(
            "type" -> dp.value.kairosType,
            "value" -> dp.value,
            "timestamp" -> instant2kairosLong(dp.timestamp)
          )
        case dp: DataPointWithMultipleValues =>
          Json.obj(
            "type" -> dp.values
              .map(_._2)
              .groupBy(_.kairosType)
              .map(t => (t._1, t._2.size))
              .maxBy(_._2)
              ._1,
            "datapoints" -> dp.values
          )
      }

      val nameTags = Json.obj(
        "name" -> JsString(datapoint.metricName.name),
        //      "type" -> kairosTypeForValue(datapoint.value), TODO custom types
        "tags" -> tags
      )

      nameTags ++ value ++ ttl
    }
  }

  implicit def groupByWrites[T <: GroupBy]: Writes[T] = new Writes[T] {
    override def writes(groupBy: T): JsValue = {
      val base = Json.obj("name" -> groupBy.name)

      groupBy match {
        case GroupByBins(bins) =>
          base ++ Json.obj("bins" -> bins)
        case GroupByTags(tags) =>
          base ++ Json.obj("tags" -> tags)
        case GroupByTime(rangeSize, groupCount) =>
          base ++ Json.obj(
            "range_size" -> rangeSize,
            "group_count" -> groupCount.toString
          )
        case GroupByValue(rangeSize) =>
          base ++ Json.obj("range_size" -> rangeSize)
        case GroupByType(typeName) =>
          base ++ Json.obj("type" -> typeName)
      }
    }
  }

  implicit val kairosTimeUnitFormat:Format[KairosTimeUnit]= new Format[KairosTimeUnit] {
    override def writes(o: KairosTimeUnit): JsValue = o match {
        case YEARS        => JsString("years")
        case MONTHS       => JsString("months")
        case WEEKS        => JsString("weeks")
        case DAYS         => JsString("days")
        case HOURS        => JsString("hours")
        case MINUTES      => JsString("minutes")
        case SECONDS      => JsString("seconds")
        case MILLISECONDS => JsString("milliseconds")
      }

    override def reads(json: JsValue): JsResult[KairosTimeUnit] = json.validate[String].map(_.toLowerCase).flatMap {
        case "years" => JsSuccess(YEARS)
        case "months" => JsSuccess(MONTHS)
        case "weeks" => JsSuccess(WEEKS)
        case "days" => JsSuccess(DAYS)
        case "hours" => JsSuccess(HOURS)
        case "minutes" => JsSuccess(MINUTES)
        case "seconds" => JsSuccess(SECONDS)
        case "milliseconds" => JsSuccess(MILLISECONDS)
        case _ => JsError(
          "unit must be one of: milliseconds, seconds, minutes, hours, days, weeks, months or years"
        )
      }
  }
  implicit val timeRangeFormat: Format[TimeRange] =    new Format[TimeRange] {

    override def writes(o: TimeRange): JsValue = Json.obj(
      "value" -> o.amount.toString, "unit" -> o.unit
    )

    override def reads(json: JsValue): JsResult[TimeRange] = {
        val unitRes = (json \ "unit").validate[KairosTimeUnit]
        val valueRes = (json \ "value").validate[Long].orElse{
          (json \ "value").validate[String].flatMap{ s =>
            Try(s.toLong) match {
              case Success(value) => JsSuccess(value)
              case Failure(exception) => JsError("error.expected.jsnumber")
            }
          }
        }

        for {
          unit <- unitRes
          value <- valueRes
        } yield {
          TimeRange(value, unit)
        }
      }
    }



  implicit val groupByReads: Reads[GroupBy] = new Reads[GroupBy] {
    override def reads(json: JsValue): JsResult[GroupBy] = {
      (json \ "name").validate[String] flatMap {
        case "tag" =>
          (json \ "tags").validate[Seq[String]] map GroupByTags
        case "time" =>
          for {
            rangeSize <- (json \ "range_size").validate[TimeRange]
            groupCount <- (json \ "group_count").validate[String] // not sure if this is correct
          } yield {
            GroupByTime(rangeSize, groupCount.toInt)
          }
        case "value" =>
          (json \ "range_size").validate[Int] map GroupByValue
        case "bin" =>
          (json \ "bins").validate[Seq[String]] map GroupByBins
        case "type" =>
          (json \ "type").validate[String] map GroupByType
      }
    }
  }

  implicit val kairosCompatibleTypeWrites: Writes[KairosCompatibleType] =
    new Writes[KairosCompatibleType] {
      override def writes(o: KairosCompatibleType): JsValue = {
        o match {
          case KNumber(value) => JsNumber(value)
          case KString(value) => JsString(value)
          case KNull          => JsNull
        }
      }
    }

  implicit def aggregatorWrites[T <: Aggregator]: Writes[T] = new Writes[T] {

    override def writes(agg: T): JsValue = {
      agg match {

        case diff: Diff =>
          Json.obj("name" -> diff.name)

        case divide: Divide =>
          Json.obj("name" -> divide.name, "divisor" -> JsNumber(divide.divisor))

        case saveAs: SaveAs =>
          Json.obj(
            "name" -> saveAs.name,
            "metric_name" -> saveAs.metricName.name,
            "tags" -> tags2json(saveAs.tags),
            "ttl" -> timeRange2ttl(saveAs.ttl)
          )

        case scale: Scale =>
          Json.obj("name" -> scale.name, "factor" -> scale.factor)

        case trim: Trim =>
          Json.obj("name" -> trim.name, "trim" -> trim.trimWhat.value)

        case rate: Rate =>
          rateAggregatorWrites.writes(rate)

        case sampler: Sampler =>
          samplerAggregatorWrites.writes(sampler)

        case percentileAgg: Percentile =>
          percentileAggregatorWrites.writes(percentileAgg)

        case rangeAgg: RangeAggregator =>
          rangeAggregatorWrites.writes(rangeAgg)

        case filter: Filter =>
          Json.obj(
            "name" -> filter.name,
            "filter_op" -> filter.operator,
            "threshold" -> filter.threshold
          )
      }
    }

    private def tags2json(tags: Seq[Tag]) = JsObject(
      tags.map(tag => (tag.name, JsString(tag.value)))
    )
  }

  implicit val samplerAggregatorWrites = new Writes[Sampler] {
    override def writes(sampler: Sampler): JsValue = {
      val base = Json.obj(
        "name" -> sampler.name,
        "unit" -> unitName(sampler.unit)
      )
      val tz = sampler.timezone.fold(Json.obj())(x =>
        Json.obj(
          "time_zone" -> x
        )
      )
      base ++ tz
    }
  }

  implicit val rateAggregatorWrites = new Writes[Rate] {
    override def writes(rate: Rate): JsValue = {
      val base = Json.obj(
        "name" -> rate.name,
        "unit" -> unitName(rate.unit),
        "sampling" -> Json.toJson(rate.sampling)
      )
      val ts = rate.timezone.fold(Json.obj())(x => Json.obj("time_zone" -> x))
      base ++ ts
    }
  }

  implicit val rangeAggregatorWrites = new Writes[RangeAggregator] {
    override def writes(rangeAgg: RangeAggregator): JsValue = {
      Json.obj(
        "name" -> rangeAgg.name,
        "sampling" -> Json.toJson(rangeAgg.sampling)
      ) ++
        rangeAgg.timeZone
          .map(tz => Json.obj("time_zone" -> tz))
          .getOrElse(Json.obj()) ++
        rangeAgg.align
          .map {
            // see https://github.com/kairosdb/kairosdb/issues/675
            case AlignStartTime => Json.obj("align_start_time" -> true, "align_sampling" -> false)
            case AlignSampling  => Json.obj("align_sampling" -> true)
          }
          .getOrElse(Json.obj()) ++
        rangeAgg.startTime
          .map(x => Json.obj("start_time" -> Json.toJson(x)))
          .getOrElse(Json.obj())
    }
  }

  implicit val percentileAggregatorWrites = new Writes[Percentile] {
    override def writes(percentileAgg: Percentile): JsValue = {
      Json.obj(
        "name" -> percentileAgg.name,
        "sampling" -> Json.toJson(percentileAgg.sampling),
        "percentile" -> percentileAgg.percentile
      ) ++
        percentileAgg.timeZone
          .map(tz => Json.obj("time_zone" -> tz))
          .getOrElse(Json.obj()) ++
        percentileAgg.align
          .map {
            // see https://github.com/kairosdb/kairosdb/issues/675
            case AlignStartTime => Json.obj("align_start_time" -> true, "align_sampling" -> false)
            case AlignSampling  => Json.obj("align_sampling" -> true)
          }
          .getOrElse(Json.obj()) ++
        percentileAgg.startTime
          .map(x => Json.obj("start_time" -> Json.toJson(x)))
          .getOrElse(Json.obj())
    }
  }

  implicit def timePointWrites[T<: TimePoint]: Writes[T] = new Writes[T] {
    override def writes(timePoint: T): JsValue = {
      timePoint match {
        case time: AbsoluteStartTime => JsNumber(time.startTime.toEpochMilli)
        case time: AbsoluteEndTime   => JsNumber(time.endTime.toEpochMilli)
        case time: RelativeStartTime =>          Json.toJson(time.howLongAgo)
        case time: RelativeEndTime =>          Json.toJson(time.howLongAgo)
      }
    }
  }

  implicit val queryMetricsWrites: Writes[QueryMetrics] =
    new Writes[QueryMetrics] {
      override def writes(queryMetrics: QueryMetrics): JsValue = {
        val plugins: Seq[(String, JsValue)] =
          if (queryMetrics.plugins.nonEmpty) {
            Seq("plugins" -> Json.toJson(queryMetrics.plugins))
          } else {
            Seq.empty
          }

        val fields: Seq[(String, JsValue)] = Seq(
          queryMetrics.timeSpan.startTime.fieldName -> Json.toJson(
            queryMetrics.timeSpan.startTime
          ),
          "metrics" -> JsArray(queryMetrics.metrics.map(x => Json.toJson(x)))
        ) ++ Seq(
          queryMetrics.timeSpan.endTime.map(x => x.fieldName -> Json.toJson(x)),
          queryMetrics.timeZone.map("time_zone" -> JsString(_)),
          queryMetrics.cacheTime.map("cache_time" -> JsNumber(_))
        ).flatten ++ plugins

        JsObject(fields)
      }
    }

  implicit val orderWrites = new Writes[Order] {
    override def writes(order: Order): JsValue = Json.toJson(order.value)
  }

  implicit val queryTagFormat = Json.format[QueryTag]

  implicit val queryPluginWrites: Writes[QueryPlugin] =
    new Writes[QueryPlugin] {
      override def writes(plugin: QueryPlugin): JsValue = {
        JsObject(
          Seq("name" -> JsString(plugin.name)) ++ plugin.properties.map(
            prop => {
              val propValue: JsValue = prop._2 match {
                case s: String              => Json.toJson(s)
                case l: Long                => Json.toJson(l)
                case i: Integer             => Json.toJson(i.longValue())
                case d: Double              => Json.toJson(d)
                case stringSeq: Seq[String] => Json.toJson(stringSeq)
              }
              prop._1 -> propValue
            }
          )
        )
      }
    }

  implicit val queryWrites: Writes[Query] = new Writes[Query] {
    override def writes(query: Query): JsValue = {
      val tags = if (query.tags.isEmpty) {
        Json.obj()
      } else {
        Json.obj(
          "tags" -> query.tags
            .map(tag => Json.obj(tag.name -> tag.allowedValues))
            .reduce((x, y) => x ++ y)
        )
      }

      val aggregators = if (query.aggregators.isEmpty) {
        Json.obj()
      } else {
        Json.obj("aggregators" -> query.aggregators)
      }

      val limit = query.limit.fold(Json.obj())(lim => Json.obj("limit" -> lim))

      val groupBys = if (query.groupBys.isEmpty) {
        Json.obj()
      } else {
        Json.obj("group_by" -> Json.toJson(query.groupBys))
      }

      val excludeTags = if (query.excludeTags) {
        Json.obj("exclude_tags" -> query.excludeTags)
      } else {
        Json.obj()
      }

      val order = if (query.order == Order.defaultOrder) {
        Json.obj()
      } else {
        Json.obj("order" -> query.order.value)
      }

      val name = Json.obj("name" -> query.metricName.name)

      val plugins = if (query.plugins.isEmpty) {
        Json.obj()
      } else {
        Json.obj("plugins" -> query.plugins)
      }

      name ++ limit ++ tags ++ aggregators ++ groupBys ++ excludeTags ++ order ++ plugins
    }
  }

  implicit val tagResultFormat = Json.format[TagResult]

  implicit val tagResultSeqReads = new Reads[Seq[TagResult]] {
    override def reads(json: JsValue) = {
      json.validate[JsObject] flatMap { obj =>
        JsArray(obj.fields map {
          case (key, maybeValues) =>
            Json.obj("name" -> key, "values" -> maybeValues)
        }).validate[Seq[TagResult]]
      }

    }
  }

  implicit val kairosCompatibleTypeReads: Reads[KairosCompatibleType] =
    new Reads[KairosCompatibleType] {
      override def reads(json: JsValue): JsResult[KairosCompatibleType] = {
        json.validate[String].map(KString) orElse json
          .validate[BigDecimal]
          .map(KNumber) orElse JsError("error.expected.jsstringOrJsnumber")
      }
    }

  implicit val dataPointValueReads: Reads[(Instant, KairosCompatibleType)] =
    new Reads[(Instant, KairosCompatibleType)] {
      override def reads(
        json: JsValue
      ): JsResult[(Instant, KairosCompatibleType)] = {
        val millisRes = json(0).validate[Long]
        val valueRes = json(1).validateOpt[KairosCompatibleType].map {
          case None        => KNull
          case Some(value) => value
        }

        for {
          millis <- millisRes
          value <- valueRes
        } yield (Instant.ofEpochMilli(millis), value)
      }
    }

  implicit val metricNameAsStringReads: Reads[MetricName] =
    new Reads[MetricName] {
      override def reads(json: JsValue) = json.validate[String].map(MetricName)
    }

  implicit val resultReads = (
    (JsPath \ "name").read[MetricName] and
      (JsPath \ "group_by")
        .read[Seq[GroupBy]]
        .orElse(new Reads[Seq[GroupBy]] {
          // return empty seq if path not found
          override def reads(json: JsValue) = JsSuccess(Seq.empty[GroupBy])
        }) and
      (JsPath \ "tags").read[Seq[TagResult]] and
      (JsPath \ "values").read[Seq[(Instant, KairosCompatibleType)]]
  )(Result.apply _)

  implicit val responseQueryReads = (
    (JsPath \ "sample_size").read[Int] and
      (JsPath \ "results").read[Seq[Result]]
  )(ResponseQuery.apply _)

  implicit val responseReads = Json.reads[Response]

  implicit val tagsResultReads = (
    (JsPath \ "name").read[MetricName] and
      (JsPath \ "tags").read[Seq[TagResult]]
  )(TagsResult.apply _)

  implicit val responseTagsReads = Json.reads[TagsResponse]
  implicit val tagResponseReads = Json.reads[TagQueryResponse]

  private def instant2kairosLong(instant: Instant): Long = instant.toEpochMilli

  private def unitName(unit: TimeUnit): String = {
    unit match {
      case TimeUnit.DAYS => "days"
      case TimeUnit.HOURS => "hours"
      case TimeUnit.MINUTES => "minutes"
      case TimeUnit.SECONDS => "seconds"
      case TimeUnit.MILLISECONDS => "milliseconds"
      case TimeUnit.NANOSECONDS | TimeUnit.MICROSECONDS =>
        throw new IllegalArgumentException("KairosDB does not support nanoseconds and microseconds")
    }
  }

  private def toJavaDuration(timeRange: TimeRange) = timeRange.unit match {
    case TimeRange.YEARS        => java.time.Duration.ofDays(timeRange.amount * 365)
    case TimeRange.MONTHS       => java.time.Duration.ofDays(timeRange.amount * 31)
    case TimeRange.WEEKS        => java.time.Duration.ofDays(timeRange.amount * 7)
    case TimeRange.DAYS         => java.time.Duration.ofDays(timeRange.amount)
    case TimeRange.HOURS        => java.time.Duration.ofHours(timeRange.amount)
    case TimeRange.MINUTES      => java.time.Duration.ofMinutes(timeRange.amount)
    case TimeRange.SECONDS      => java.time.Duration.ofSeconds(timeRange.amount)
    case TimeRange.MILLISECONDS => java.time.Duration.ofMillis(timeRange.amount)
  }

  private def timeRange2ttl(dur: TimeRange): Long = {
    // if value is > 0 and < 1 second, set it to a second or KairosDB will not set a TTL
    dur match {
      case TimeRange(x, MILLISECONDS) if Math.abs(x) < 1000 => 1
      case TimeRange(0, _)                                  => 0
      case x: TimeRange =>
        toJavaDuration(x).getSeconds
    }
  }
}

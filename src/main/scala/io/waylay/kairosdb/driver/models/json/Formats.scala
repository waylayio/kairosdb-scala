package io.waylay.kairosdb.driver.models.json

import java.time.Instant
import java.util.concurrent.TimeUnit

import io.waylay.kairosdb.driver.models.GroupBy._
import io.waylay.kairosdb.driver.models.KairosCompatibleType.{KNumber, KString}
import io.waylay.kairosdb.driver.models.{Aggregator, KairosCompatibleType, RangeAggregator, _}
import io.waylay.kairosdb.driver.models.Aggregator._
import io.waylay.kairosdb.driver.models.KairosQuery.{Order, QueryTag}
import io.waylay.kairosdb.driver.models.QueryMetricTagsResponse.{TagsResponse, TagsResult}
import io.waylay.kairosdb.driver.models.QueryResponse.{Response, ResponseQuery, Result, TagResult}
import io.waylay.kairosdb.driver.models.RangeAggregator.Align.{AlignSampling, AlignStartTime}
import io.waylay.kairosdb.driver.models.TimeSpan._
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.concurrent.duration._

object Formats {

  implicit val datapointWrites: Writes[DataPoint] = new Writes[DataPoint] {

    implicit val datapointWithTimeStampWrites = new Writes[(Instant, KairosCompatibleType)] {
      override def writes(point: (Instant, KairosCompatibleType)): JsValue = {
        val (time, value) = point
        Json.arr(
          time.toEpochMilli,
          value
        )
      }
    }

    override def writes(datapoint: DataPoint): JsValue = {
      val tags = JsObject(
        datapoint.tags.map(tag =>
          (tag.name, JsString(tag.value))
        ).toMap
      )

      val ttl = datapoint.ttl.fold(Json.obj())(x =>
        Json.obj(
          "ttl" -> finiteDuration2ttl(x)
        )
      )

      val value: JsObject = datapoint match {
        case dp: DataPointWithSingleValue =>
          Json.obj(
            "value" -> dp.value,
            "timestamp" -> instant2kairosLong(dp.timestamp)
          )
        case dp: DataPointWithMultipleValues =>
          Json.obj(
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

  implicit val groupByWrites: Writes[GroupBy] = new Writes[GroupBy] {
    override def writes(groupBy: GroupBy): JsValue = {
      val base = Json.obj(
        "name" -> groupBy.name
      )

      groupBy match {
        case GroupByBins(bins) =>
          base ++ Json.obj(
            "bins" -> bins
          )
        case GroupByTags(tags) =>
          base ++ Json.obj(
            "tags" -> tags
          )
        case GroupByTime(rangeSize, groupCount) =>
          base ++ Json.obj(
            "range_size" -> rangeSize,
            "group_count" -> groupCount.toString
          )
        case GroupByValue(rangeSize) =>
          base ++ Json.obj(
            "range_size" -> rangeSize
          )
        case GroupByType(typeName) =>
          base ++ Json.obj(
            "type" -> typeName
          )
      }
    }
  }

  implicit val finiteDurationReads: Reads[FiniteDuration] = new Reads[FiniteDuration] {
    override def reads(json: JsValue): JsResult[FiniteDuration] = {
      def fromString(input: String): Option[Long => FiniteDuration] = {
        input match {
          case "milliseconds" => Some((x: Long) => x.milliseconds)
          case "seconds" => Some((x: Long) => x.seconds)
          case "minutes" => Some((x: Long) => x.minutes)
          case "hours" => Some((x: Long) => x.hours)
          case "days" => Some((x: Long) => x.days)
          case "weeks" => Some((x: Long) => (x * 7).days)
          case "months" => None // not supported by scala.concurrent.duration but supported by KairosDB
          case "years" => None // not supported by scala.concurrent.duration but supported by KairosDB
          case _ => None
        }
      }

      val unitRes = (json \ "unit").validate[String].map(_.toLowerCase).flatMap { x =>
        fromString(x)
          .map(fun => JsSuccess.apply(fun))
          .getOrElse(JsError("unit must be one of: milliseconds, seconds, minutes, hours, days, weeks. (months and years are supported by KairosDB but not this driver)"))
      }
      val valueRes = (json \ "value").validate[Int]

      for {
        unit <- unitRes
        value <- valueRes
      } yield { unit(value) }
    }
  }

  implicit val groupByReads: Reads[GroupBy] = new Reads[GroupBy] {
    override def reads(json: JsValue): JsResult[GroupBy] = {
      (json \ "name").validate[String] flatMap {
        case "tag" =>
          (json \ "tags").validate[Seq[String]] map GroupByTags
        case "time" =>
          for {
            rangeSize <- (json \ "range_size").validate[FiniteDuration]
            groupCount <- (json \ "group_count").validate[String] // not sure if this is correct
          } yield { GroupByTime(rangeSize, groupCount.toInt) }
        case "value" =>
          (json \ "range_size").validate[Int] map GroupByValue
        case "bin" =>
          (json \ "bins").validate[Seq[String]] map GroupByBins
        case "type" =>
          (json \ "type").validate[String] map GroupByType
      }
    }
  }

  implicit val kairosCompatibleTypeWrites: Writes[KairosCompatibleType] = new Writes[KairosCompatibleType] {
    override def writes(o: KairosCompatibleType): JsValue = {
      o match {
        case KNumber(value) => JsNumber(value)
        case KString(value) => JsString(value)
      }
    }
  }

  implicit val aggregatorWrites: Writes[Aggregator] = new Writes[Aggregator] {

    override def writes(agg: Aggregator): JsValue = {
      agg match {

        case diff: Diff =>
          Json.obj(
            "name" -> diff.name
          )

        case divide: Divide =>
          Json.obj(
            "name" -> divide.name,
            "divisor" -> JsNumber(divide.divisor)
          )

        case saveAs: SaveAs =>
          Json.obj(
            "name" -> saveAs.name,
            "metric_name" -> saveAs.metricName.name,
            "tags" -> tags2json(saveAs.tags),
            "ttl" ->  finiteDuration2ttl(saveAs.ttl)
          )

        case scale: Scale =>
          Json.obj(
            "name" -> scale.name,
            "factor" -> scale.factor
          )

        case trim: Trim =>
          Json.obj(
            "name" -> trim.name,
            "trim" -> trim.trimWhat.value
          )

        case rate: Rate =>
          rateAggregatorWrites.writes(rate)

        case sampler: Sampler =>
          samplerAggregatorWrites.writes(sampler)

        case percentileAgg: Percentile =>
          percentileAggregatorWrites.writes(percentileAgg)

        case rangeAgg: RangeAggregator =>
          rangeAggregatorWrites.writes(rangeAgg)
      }
    }

    private def tags2json(tags: Seq[Tag]) = JsObject(
      tags.map(tag =>
        (tag.name, JsString(tag.value))
      )
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

  implicit val rateAggregatorWrites = new Writes[Rate]{
    override def writes(rate: Rate): JsValue = {
      val base = Json.obj(
        "name" -> rate.name,
        "unit" -> unitName(rate.unit),
        "sampling" -> Json.toJson(rate.sampling)
      )
      val ts = rate.timezone.fold(Json.obj())(x =>
        Json.obj(
          "time_zone" -> x
        )
      )
      base ++ ts
    }
  }

  implicit val rangeAggregatorWrites = new Writes[RangeAggregator]{
    override def writes(rangeAgg: RangeAggregator): JsValue = {
      Json.obj(
        "name" -> rangeAgg.name,
        "sampling" -> Json.toJson(rangeAgg.sampling)
      ) ++
        rangeAgg.timeZone.map(tz => Json.obj("time_zone" -> tz)).getOrElse(Json.obj()) ++
        rangeAgg.align.map {
          case AlignStartTime => Json.obj("align_start_time" -> true)
          case AlignSampling => Json.obj("align_sampling" -> true)
        }.getOrElse(Json.obj()) ++
        rangeAgg.startTime.map(x => Json.obj("start_time" -> Json.toJson(x))).getOrElse(Json.obj())
    }
  }

  implicit val percentileAggregatorWrites = new Writes[Percentile]{
    override def writes(percentileAgg: Percentile): JsValue = {
      Json.obj(
        "name" -> percentileAgg.name,
        "sampling" -> Json.toJson(percentileAgg.sampling),
        "percentile" -> percentileAgg.percentile
      ) ++
        percentileAgg.timeZone.map(tz => Json.obj("time_zone" -> tz)).getOrElse(Json.obj()) ++
        percentileAgg.align.map {
          case AlignStartTime => Json.obj("align_start_time" -> true)
          case AlignSampling => Json.obj("align_sampling" -> true)
        }.getOrElse(Json.obj()) ++
        percentileAgg.startTime.map(x => Json.obj("start_time" -> Json.toJson(x))).getOrElse(Json.obj())
    }
  }

  implicit val timePointWrites: Writes[TimePoint] = new Writes[TimePoint] {
    override def writes(timePoint: TimePoint): JsValue = {
      timePoint match {
        case time: AbsoluteStartTime => JsNumber(time.startTime.toEpochMilli)
        case time: AbsoluteEndTime => JsNumber(time.endTime.toEpochMilli)
        case time: RelativeStartTime => Json.toJson(time.howLongAgo)(finiteDurationWrites)
        case time: RelativeEndTime => Json.toJson(time.howLongAgo)(finiteDurationWrites)
      }
    }
  }

  implicit val finiteDurationWrites: Writes[FiniteDuration] = new Writes[FiniteDuration] {
    override def writes(o: FiniteDuration): JsValue = {
      Json.obj(
        "unit" -> unitName(o.unit),
        "value" -> o.length.toString
      )
    }
  }

  implicit val queryMetricsWrites: Writes[QueryMetrics] = new Writes[QueryMetrics] {
    override def writes(queryMetrics: QueryMetrics): JsValue = {
      val fields: Seq[(String, JsValue)] = Seq(
        queryMetrics.timeSpan.startTime.fieldName -> Json.toJson(queryMetrics.timeSpan.startTime),
        "metrics" -> JsArray(queryMetrics.metrics.map(x => Json.toJson(x)))
      ) ++ Seq(
        queryMetrics.timeSpan.endTime.map(x => x.fieldName -> Json.toJson(x)),
        queryMetrics.timeZone.map("time_zone" -> JsString(_)),
        queryMetrics.cacheTime.map("cache_time" -> JsNumber(_))
      ).flatten

      JsObject(fields)
    }
  }

  implicit val orderWrites = new Writes[Order] {
    override def writes(order: Order): JsValue = Json.toJson(order.value)
  }

  implicit val queryTagFormat = Json.format[QueryTag]

  implicit val queryWrites: Writes[Query] = new Writes[Query] {
    override def writes(query: Query): JsValue = {
      val tags = if(query.tags.isEmpty) {
        Json.obj()
      } else {
        Json.obj(
          "tags" -> query.tags.map(tag =>
            Json.obj(
              tag.name -> tag.allowedValues
            )
          ).reduce((x,y) => x ++ y)
        )
      }

      val aggregators = if(query.aggregators.isEmpty){
        Json.obj()
      } else {
        Json.obj(
          "aggregators" -> query.aggregators
        )
      }

      val limit = query.limit.fold(Json.obj())(lim =>
        Json.obj(
          "limit" -> lim
        )
      )

      val groupBys = if(query.groupBys.isEmpty) {
        Json.obj()
      } else {
        Json.obj(
          "group_by" -> Json.toJson(query.groupBys)
        )
      }

      val excludeTags = if(query.excludeTags){
        Json.obj(
          "exclude_tags" -> query.excludeTags
        )
      }else{
        Json.obj()
      }

      val order = if(query.order == Order.defaultOrder) {
        Json.obj()
      } else {
        Json.obj(
          "order" -> query.order.value
        )
      }

      val name = Json.obj(
        "name" -> query.metricName.name
      )

      name ++ limit ++ tags ++ aggregators ++ groupBys ++ excludeTags ++ order
    }
  }

  implicit val tagResultFormat = Json.format[TagResult]

  implicit val tagResultSeqReads = new Reads[Seq[TagResult]] {
    override def reads(json: JsValue) = {
      json.validate[JsObject] flatMap { obj =>
        JsArray(obj.fields map  { case (key, maybeValues) => Json.obj("name" -> key, "values" -> maybeValues) }).validate[Seq[TagResult]]
      }

    }
  }

  implicit val kairosCompatibleTypeReads: Reads[KairosCompatibleType] = new Reads[KairosCompatibleType] {
    override def reads(json: JsValue): JsResult[KairosCompatibleType] = {
      val bigdecimalRes = json.validate[BigDecimal]
      val stringRes = json.validate[String]

      bigdecimalRes map KNumber orElse stringRes.map(KString) orElse JsError(ValidationError("error.expected.jsstringOrJsnumber"))
    }
  }

  implicit val dataPointValueReads: Reads[(Instant, KairosCompatibleType)] = new Reads[(Instant, KairosCompatibleType)] {
    override def reads(json: JsValue): JsResult[(Instant, KairosCompatibleType)] = {
      val millisRes = json(0).validate[Long]
      val valueRes = json(1).validate[KairosCompatibleType]

      for {
        millis <- millisRes
        value  <- valueRes
      } yield (Instant.ofEpochMilli(millis), value)
    }
  }

  implicit val metricNameAsStringReads: Reads[MetricName] = new Reads[MetricName] {
    override def reads(json: JsValue) = json.validate[String].map(MetricName)
  }

  implicit val resultReads = (
      (JsPath \ "name").read[MetricName] and
      (JsPath \ "group_by").read[Seq[GroupBy]].orElse(new Reads[Seq[GroupBy]] {
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

  private def finiteDuration2ttl(dur: FiniteDuration): Long = {
    // if value is > 0 and < 1 second, set it to a second or KairosDB will not set a TTL
    if(dur.toSeconds != 0) dur.toSeconds
    else if(dur.toSeconds == 0 && dur != 0.seconds) 1
    else 0
  }
}

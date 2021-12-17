package io.waylay.kairosdb.driver.models

import java.time.Instant
import io.lemonlabs.uri.{Uri, Url, UrlWithAuthority}
import io.waylay.kairosdb.driver.models.KairosQuery.{Order, QueryTag}
import io.waylay.kairosdb.driver.models.QueryResponse.TagResult
import io.waylay.kairosdb.driver.models.TimeRange.KairosTimeUnit

import scala.collection.immutable.Seq
import scala.collection.compat._

/**
  * Metric names are case sensitive and can only contain the following characters: alphanumeric characters, period ”.”,
  * slash “/”, dash “-”, and underscore “_” (not enforced)
  */
case class MetricName(name: String) extends AnyVal

object TimeRange {
  sealed trait KairosTimeUnit
  case object MILLISECONDS extends KairosTimeUnit
  case object SECONDS extends KairosTimeUnit
  case object MINUTES extends KairosTimeUnit
  case object HOURS extends KairosTimeUnit
  case object DAYS extends KairosTimeUnit
  case object WEEKS extends KairosTimeUnit
  case object MONTHS extends KairosTimeUnit
  case object YEARS extends KairosTimeUnit
}
case class TimeRange(amount: Long, unit: KairosTimeUnit)

sealed trait DataPoint {
  val metricName: MetricName
  val tags: Seq[Tag]
  val ttl: Option[TimeRange]
}

object DataPoint {
  def apply(metricName: MetricName, value: KairosCompatibleType, timestamp: Instant = Instant.now, tags: Seq[Tag],
    ttl: Option[TimeRange] = None) = DataPointWithSingleValue(metricName, value, timestamp, tags, ttl)
}

/**
  * A data point has with metric name, a single value, a timestamp, and a list of one or more tags
  *
  * @param ttl Sets the Cassandra ttl for the data points. None or Some(0.seconds) will not set a TTL
  *
  */
case class DataPointWithSingleValue(metricName: MetricName, value: KairosCompatibleType, timestamp: Instant,
  tags: Seq[Tag], ttl: Option[TimeRange] = None) extends DataPoint

/**
  * A data point has with metric name, a single value, a timestamp, and a list of one or more tags
  *
  * @param ttl Sets the Cassandra ttl for the data points. None or Some(0.seconds) will not set a TTL
  */
case class DataPointWithMultipleValues(metricName: MetricName, values: Seq[(Instant, KairosCompatibleType)],
  tags: Seq[Tag] = Seq.empty, ttl: Option[TimeRange] = None) extends DataPoint


/**
  * Tags are named properties that identify the data, such as its type and where it comes from
  *
  * Tag names and values are case sensitive and can only contain the following characters: alphanumeric characters,
  * period ”.”, slash “/”, dash “-”, and underscore “_” (not enforced)
  */
case class Tag(name: String, value: String)

/** Kairos base URL */
case class KairosDBConfig(url: Url)

object KairosDBConfig {
  def apply(
    scheme: String = "http",
    host: String = "localhost",
    port: Int = 8080,
    username: Option[String] = None,
    password: Option[String] = None
  ): KairosDBConfig = {
    val baseUri = Url()
      .withScheme(scheme)
      .withHost(host)
      .withPort(port)

    val uriWithCredentialsOpt = for {
      user <- username
      pass <- password
    } yield {
      baseUri.toAbsoluteUrl.withUser(user).withPassword(pass)
    }

    val serverUri = uriWithCredentialsOpt getOrElse baseUri

    KairosDBConfig(serverUri)
  }

  def apply(javaUri: java.net.URI): KairosDBConfig = KairosDBConfig(Uri(javaUri).toUrl)
}

/**
  * KairosDB provides REST APIs that show the health of the system.
  *
  * There are currently two health checks executed for each API.
  *
  *  - The JVM thread deadlock check verifies that no deadlocks exist in the KairosDB JVM.
  *  - The Datastore query check performs a query on the data store to ensure that the data store is responding.
  *
  *  Example value: HealthStatusResults(Seq("JVM-Thread-Deadlock: OK", "Datastore-Query: OK"))
  */
case class HealthStatusResults(results: Seq[String])

sealed trait HealthCheckResult

object HealthCheckResult {
  case object AllHealthy extends HealthCheckResult
  case object Unhealthy extends HealthCheckResult
}

/** KairosDB only supports numbers and strings. Custom types can be defined */
sealed trait KairosCompatibleType {
  def kairosType: String
}

object KairosCompatibleType {
  case object KNull extends KairosCompatibleType {
    override def kairosType: String = "string"
  }

  case class KNumber(value: BigDecimal) extends KairosCompatibleType {
    def kairosType:String = {
      //TODO check if this is 100% correct
      if (value.scale > 0) {
        "double"
      } else {
        "long"
      }
    }
  }
  case class KString(value: String) extends KairosCompatibleType {
    val kairosType:String = "string"
  }
}

object QueryResponse {
  case class Response(queries: Seq[ResponseQuery])
  case class ResponseQuery(sampleSize: Int, results: Seq[Result])
  case class Result(name: MetricName, groupBy: Seq[GroupBy], tags: Seq[TagResult], values: Seq[(Instant, KairosCompatibleType)])
  case class TagResult(name: String, values: Seq[String]) // not sure if this is the correct meaning
}

object QueryMetricTagsResponse {
  case class TagQueryResponse(queries: Seq[TagsResponse])
  case class TagsResponse(results: Seq[TagsResult])
  case class TagsResult(name: MetricName, tags: Seq[TagResult])
}

object KairosQuery {
  /**
    * Used to filter a query. With `QueryTag("aoeu", Seq("snth", "htns"))` you would only get results where the value of
    * tag `aoeu` is `snth` or `htns`
    */
  case class QueryTag(name: String, allowedValues: Seq[String])

  object QueryTag {
    //def apply(name: String, allowedValue: String): QueryTag = QueryTag(name, Seq(allowedValue))
    def apply(tuple: (String, String)): QueryTag = QueryTag(tuple._1, Seq(tuple._2))
    def apply(tuple: (String, String)*): Seq[QueryTag] = tuple.map(tup => QueryTag(tup)).to(Seq)
  }

  sealed trait Order { val value: String }

  object Order {
    case object Ascending extends Order { override val value = "asc" }
    case object Descending extends Order { override val value = "desc"}
    val defaultOrder = Ascending
  }
}

/** @param tags Tags narrow down the search. Only metrics that include the tag and matches one of the values are
  *             returned. Tags are optional.
  * @param groupBys The resulting data points can be grouped by one or more tags, a time range, or by value, or by a
  *                 combination of the three.
  * @param aggregators An ordered array of aggregators. They are processed in the order specified. The output of an
  *                    aggregator is passed to the input of the next until all have been processed.
  * @param limit Limits the number of data points returned from the data store.
  *              The limit is applied before any aggregator is executed.
  * @param order Orders the returned data points. This sorting is done before any aggregators are executed.
  * @param excludeTags By default, the result of the query includes tags and tag values associated with the data points.
  *                    If `excludeTags` is set to true, the tags will be excluded from the response.
  * @param plugins optional plugin references to customize the behavior of the query on this metric
  */
case class Query(
  metricName: MetricName,
  tags: Seq[QueryTag] = Seq.empty,
  groupBys: Seq[GroupBy] = Seq.empty,
  aggregators: Seq[Aggregator] = Seq.empty,
  limit: Option[Int] = None,
  order: Order = Order.defaultOrder,
  excludeTags: Boolean = false,
  plugins: Seq[QueryPlugin] = Seq.empty)

/** @param timeZone The time zone for the time range of the query. If not specified, UTC is used. tz format, e.g. "Europe/Brussels"
  * @param cacheTime The amount of time in seconds to re use the cache from a previous query. When a query is made,
  *                  Kairos looks for the cache file for the query. If a cache file is found and the timestamp of the
  *                  cache file is within cache_time seconds from the current query, the cache is used.
  *                  Sending a query with a cacheTime set to 0 will always refresh the cache with new data from Cassandra.
  * @param plugins optional plugin references to custom the behavior of this query
  */
case class QueryMetrics(
  metrics: Seq[Query],
  timeSpan: TimeSpan,
  timeZone: Option[String] = None,
  cacheTime: Option[Int] = None,
  plugins: Seq[QueryPlugin] = Seq.empty)


/**
  * Reference to a plugin which can customize the behavior of a query.
  *
  * @param name published name of the plugin
  * @param properties properties for the plugin within the query invocation
  */
case class QueryPlugin(
  name: String,
  properties: Map[String,Any] = Map.empty)

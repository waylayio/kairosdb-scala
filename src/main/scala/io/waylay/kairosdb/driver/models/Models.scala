package io.waylay.kairosdb.driver.models

import java.time.Instant

import com.netaporter.uri.Uri
import io.waylay.kairosdb.driver.models.KairosQuery.{Order, QueryTag}
import io.waylay.kairosdb.driver.models.QueryResponse.TagResult

import scala.concurrent.duration.FiniteDuration

/**
  * Metric names are case sensitive and can only contain the following characters: alphanumeric characters, period ”.”,
  * slash “/”, dash “-”, and underscore “_” (not enforced)
  */
case class MetricName(name: String) extends AnyVal

sealed trait DataPoint {
  val metricName: MetricName
  val tags: Seq[Tag]
  val ttl: Option[FiniteDuration]
}

object DataPoint {
  def apply(metricName: MetricName, value: KairosCompatibleType, timestamp: Instant = Instant.now, tags: Seq[Tag],
    ttl: Option[FiniteDuration] = None) = DataPointWithSingleValue(metricName, value, timestamp, tags, ttl)
}

/**
  * A data point has with metric name, a single value, a timestamp, and a list of one or more tags
  *
  * @param ttl Sets the Cassandra ttl for the data points. None or Some(0.seconds) will not set a TTL
  *
  */
case class DataPointWithSingleValue(metricName: MetricName, value: KairosCompatibleType, timestamp: Instant,
  tags: Seq[Tag], ttl: Option[FiniteDuration] = None) extends DataPoint

/**
  * A data point has with metric name, a single value, a timestamp, and a list of one or more tags
  *
  * @param ttl Sets the Cassandra ttl for the data points. None or Some(0.seconds) will not set a TTL
  */
case class DataPointWithMultipleValues(metricName: MetricName, values: Seq[(Instant, KairosCompatibleType)],
  tags: Seq[Tag] = Seq.empty, ttl: Option[FiniteDuration] = None) extends DataPoint


/**
  * Tags are named properties that identify the data, such as its type and where it comes from
  *
  * Tag names and values are case sensitive and can only contain the following characters: alphanumeric characters,
  * period ”.”, slash “/”, dash “-”, and underscore “_” (not enforced)
  */
case class Tag(name: String, value: String)

/** Kairos base URL */
case class KairosDBConfig(uri: Uri)

object KairosDBConfig {
  def apply(
    scheme: String = "http",
    host: String = "localhost",
    port: Int = 8080,
    username: Option[String] = None,
    password: Option[String] = None
  ): KairosDBConfig = {
    val baseUri = Uri()
      .withScheme(scheme)
      .withHost(host)
      .withPort(port)

    val uriWithCredentialsOpt = for {
      user <- username
      pass <- password
    } yield {
      baseUri.withUser(user).withPassword(pass)
    }

    val serverUri = uriWithCredentialsOpt getOrElse baseUri

    KairosDBConfig(serverUri)
  }

  def apply(javaUri: java.net.URI): KairosDBConfig = KairosDBConfig(Uri(javaUri))
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
  val value: Any
}

object KairosCompatibleType {
  case class KNumber(value: BigDecimal) extends KairosCompatibleType
  case class KString(value: String) extends KairosCompatibleType
}

object QueryResponse {
  case class Response(queries: Seq[ResponseQuery])
  case class ResponseQuery(sampleSize: Int, results: Seq[Result])
  case class Result(name: MetricName, groupBy: Seq[GroupBy], tags: Seq[TagResult], values: Seq[(Instant, KairosCompatibleType)])
  case class TagResult(name: String, values: Seq[String]) // not sure if this is the correct meaning
}

object QueryMetricTagsResponse {
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
    def apply(name: String, allowedValue: String): QueryTag = QueryTag(name, Seq(allowedValue))
    def apply(tuple: (String, String)): QueryTag = QueryTag(tuple._1, tuple._2)
    def apply(tuple: (String, String)*): Seq[QueryTag] = tuple.map(tup => QueryTag(tup))
  }

  sealed trait Order { val value: String }

  object Order {
    case class Ascending() extends Order { override val value = "asc" }
    case class Descending() extends Order { override val value = "desc"}
    val defaultOrder = Ascending()
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
  */
case class Query(
  metricName: MetricName,
  tags: Seq[QueryTag] = Seq.empty,
  groupBys: Seq[GroupBy] = Seq.empty,
  aggregators: Seq[Aggregator] = Seq.empty,
  limit: Option[Int] = None,
  order: Order = Order.defaultOrder,
  excludeTags: Boolean = false)

/** @param timeZone The time zone for the time range of the query. If not specified, UTC is used. tz format, e.g. "Europe/Brussels"
  * @param cacheTime The amount of time in seconds to re use the cache from a previous query. When a query is made,
  *                  Kairos looks for the cache file for the query. If a cache file is found and the timestamp of the
  *                  cache file is within cache_time seconds from the current query, the cache is used.
  *                  Sending a query with a cacheTime set to 0 will always refresh the cache with new data from Cassandra.
  */
case class QueryMetrics(
  metrics: Seq[Query],
  timeSpan: TimeSpan,
  timeZone: Option[String] = None,
  cacheTime: Option[Int] = None)

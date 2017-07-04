package io.waylay.kairosdb.driver

import io.waylay.kairosdb.driver.models._
import io.waylay.kairosdb.driver.models.HealthCheckResult._
import com.netaporter.uri.dsl._
import com.typesafe.scalalogging.StrictLogging
import io.waylay.kairosdb.driver.KairosDB._
import io.waylay.kairosdb.driver.models.QueryMetricTagsResponse.TagsResponse
import io.waylay.kairosdb.driver.models.json.Formats._
import io.waylay.kairosdb.driver.models.QueryResponse.Response
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.ws.{WSAuthScheme, WSClient, WSRequest, WSResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

object KairosDB {

  /** Parent exception for all exceptions this driver may throw */
  class KairosDBException(msg: String) extends Exception(msg)

  /** Parent exception for all exception this driver may throw when querying data */
  abstract class KairosDBResponseException(msg: String) extends KairosDBException(msg)

  /** Thrown when the response by KairosDB can't be parsed */
  case class KairosDBResponseParseException(msg: String = "Could not parse KairosDB response", errors: Seq[(JsPath, Seq[ValidationError])])
    extends KairosDBResponseException(s"""$msg (errors: ${errors.mkString(", ")})""")

  /** Thrown when the request is invalid */
  case class KairosDBResponseBadRequestException(msg: String = "KairosDB returned HTTP 400 (Bad Request)", errors: Seq[String])
    extends KairosDBResponseException(s"""$msg (errors: ${errors.mkString(", ")})""")

  /** Thrown when the request is unauthorized */
  case class KairosDBResponseUnauthorizedException(msg: String = "KairosDB returned HTTP 401 (Unauthorized)", errors: Seq[String])
    extends KairosDBResponseException(s"""$msg (errors: ${errors.mkString(", ")})""")

  /** Thrown if an error occurs retrieving data */
  case class KairosDBResponseInternalServerErrorException(msg: String = "KairosDB returned HTTP 500 (Internal Server Error)", errors: Seq[String])
    extends KairosDBResponseException(s"""$msg (errors: ${errors.mkString(", ")})""")

  /** Thrown if an unexpected error occurs retrieving data */
  case class KairosDBResponseUnhandledException(statusCode: Int, errors: Seq[String])
    extends KairosDBResponseException(s"""KairosDB returned unhandled HTTP $statusCode (???) (errors: ${errors.mkString(", ")})""")
}

class KairosDB(wsClient: WSClient, config: KairosDBConfig, executionContext: ExecutionContext) extends StrictLogging {
  implicit val ec = executionContext
  val uri = config.uri

  def listMetricNames: Future[Seq[MetricName]] = {
    wsClient
      .url(uri / "api" / "v1" / "metricnames")
      .applyKairosDBAuth
      .get
      .map {
        wsRepsonseToResult(json => (json \ "results").validate[Seq[String]])
      }
      .map(_.map(MetricName))
  }

  def listTagNames: Future[Seq[String]] = {
    wsClient
      .url(uri / "api" / "v1" / "tagnames")
      .applyKairosDBAuth
      .get
      .map {
        wsRepsonseToResult(json => (json \ "results").validate[Seq[String]])
      }
  }

  def listTagValues: Future[Seq[String]] = {
    wsClient
      .url(uri / "api" / "v1" / "tagvalues")
      .applyKairosDBAuth
      .get
      .map(
        wsRepsonseToResult(json => (json \ "results").validate[Seq[String]])
      )
  }

  def healthStatus: Future[HealthStatusResults] = {
    wsClient
      .url(uri / "api" / "v1" / "health" / "status")
      .applyKairosDBAuth
      .get
      .map {
        wsRepsonseToResult(_.validate[Seq[String]])
      }
      .map(HealthStatusResults)
  }

  def healthCheck: Future[HealthCheckResult] = {
    wsClient
      .url(uri / "api" / "v1" / "health" / "check")
      .withRequestTimeout(10.seconds)
      .applyKairosDBAuth
      .get
      .map(_.status)
      .map {
        case 204 => AllHealthy
        case _   => Unhealthy
      }
      .recover {
        case _ => Unhealthy
      }
  }

  def version: Future[String] = {
    wsClient
      .url(uri / "api" / "v1" / "version")
      .applyKairosDBAuth
      .get
      .map (
        wsRepsonseToResult(json => (json \ "version").validate[String])
      )
  }

  def deleteMetric(metricName: MetricName): Future[Unit] = {
    wsClient
      .url(uri / "api" / "v1" / "metric" / metricName.name)
      .applyKairosDBAuth
      .delete
      .map(emptyWsRepsonseToResult)
  }

  def addDataPoints(dataPoints: Seq[DataPoint]): Future[Unit] = {
    val body = Json.toJson(dataPoints)
    logger.debug(Json.prettyPrint(body))

    wsClient
      .url(uri / "api" / "v1" / "datapoints")
      .applyKairosDBAuth
      .post(body)
      .map(emptyWsRepsonseToResult)
  }

  def addDataPoint(dataPoint: DataPoint): Future[Unit] = addDataPoints(Seq(dataPoint))

  def queryMetrics(queryMetrics: QueryMetrics): Future[Response] = {
    val query = Json.toJson(queryMetrics)
    logger.debug(Json.prettyPrint(query))
    wsClient
      .url(uri / "api" / "v1" / "datapoints" / "query")
      .applyKairosDBAuth
      .post(query)
      .map {
        wsRepsonseToResult(_.validate[Response])
      }
  }

  /** You can think of this as the exact same as the query but it leaves off the data and just returns the tag information.
    *
    * Note: Currently this is not implemented in the HBase datastore.
    */
  def queryMetricTags(queryMetrics: QueryMetrics): Future[TagsResponse] = {
    val query = Json.toJson(queryMetrics)
    logger.debug(Json.prettyPrint(query))
    wsClient
      .url(uri / "api" / "v1" / "datapoints" / "query" / "tags")
      .applyKairosDBAuth
      .post(query)
      .map {
        wsRepsonseToResult(_.validate[TagsResponse])
      }
  }

  /** Delete will perform the query specified in the body and delete all data points returned by the query. Aggregators
    * and groupers have no effect on which data points are deleted.
    *
    * Delete is designed such that you could perform a query, verify that the data points returned are correct, and
    * issue the delete with that query.
    *
    * Note: Delete works for the Cassandra and H2 data store only.
    */
  def deleteDataPoints(queryMetrics: QueryMetrics): Future[Unit] = {
    val query = Json.toJson(queryMetrics)
    logger.debug(Json.prettyPrint(query))
    wsClient
      .url(uri / "api" / "v1" / "datapoints" / "delete")
      .applyKairosDBAuth
      .post(query)
      .map(emptyWsRepsonseToResult)
  }

  private def parseQueryResult(json: JsValue): Response = {
    json.validate[Response] match {
      case JsSuccess(res, jsPath) => res
      case JsError(errors) => throw KairosDBResponseParseException(errors = errors)
    }
  }

  private def getErrors(res: WSResponse): Seq[String] = {
    Try(res.json) match {
      case Success(json) =>
        (json \ "errors").validate[Seq[String]].asOpt.toSeq.flatten
      case Failure(_) =>
        // not a json response like jetty default 401
        Seq.empty
    }

  }

  private implicit class PimpedWSRequest(req: WSRequest) {
    def applyKairosDBAuth: WSRequest = {
      val withAuthOption = for {
        user <- uri.user
        pass <-  uri.password
      } yield req.withAuth(user, pass, WSAuthScheme.BASIC)
      withAuthOption getOrElse req
    }
  }

  private val emptyWsRepsonseToResult = {
    emptyHandling orElse PartialFunction(responseErrorHandling)
  }

  private def wsRepsonseToResult[T](transform: JsValue => JsResult[T], successStatusCode: Int = 200) = {
    withBodyHandling(transform, successStatusCode) orElse PartialFunction(responseErrorHandling)
  }

  private def emptyHandling : PartialFunction[WSResponse, Unit] = {
    case res if res.status == 204 => ()
  }

  private def withBodyHandling[T](transform: JsValue => JsResult[T], successStatusCode: Int): PartialFunction[WSResponse, T] = {
    case res if res.status == successStatusCode =>
      Try(res.json) match {
        case Success(json) =>
          transform(json) match {
            case JsSuccess(result, jsPath) => result
            case JsError(errors) => throw KairosDBResponseParseException(errors = errors)
          }
        case Failure(e) =>
          throw KairosDBResponseParseException("Failed to parse response body to json", Seq())
      }
  }

  private def responseErrorHandling[T](res: WSResponse) = {
    res.status match {
      case 400   => throw KairosDBResponseBadRequestException(errors = getErrors(res))
      case 401   => throw KairosDBResponseUnauthorizedException(errors = getErrors(res))
      case 500   => throw KairosDBResponseInternalServerErrorException(errors = getErrors(res))
      case other => throw KairosDBResponseUnhandledException(other, getErrors(res))
    }
  }


}

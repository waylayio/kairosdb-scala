package io.waylay.kairosdb.driver

import io.waylay.kairosdb.driver.models._
import io.waylay.kairosdb.driver.models.HealthCheckResult._
import com.netaporter.uri.dsl._
import com.typesafe.scalalogging.StrictLogging
import io.waylay.kairosdb.driver.KairosDB.{KairosDBResponseBadRequestException, KairosDBResponseInternalServerErrorException, KairosDBResponseParseException}
import io.waylay.kairosdb.driver.models.QueryMetricTagsResponse.TagsResponse
import io.waylay.kairosdb.driver.models.json.Formats._
import io.waylay.kairosdb.driver.models.QueryResponse.Response
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.ws.{WSAuthScheme, WSClient, WSRequest, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

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
    extends KairosDBException(s"""$msg (errors: ${errors.mkString(", ")})""")

  /** Thrown if an error occurs retrieving data */
  case class KairosDBResponseInternalServerErrorException(msg: String = "KairosDB returned HTTP 500 (Internal Server Error)", errors: Seq[String])
    extends KairosDBException(s"""$msg (errors: ${errors.mkString(", ")})""")
}

class KairosDB(wsClient: WSClient, config: KairosDBConfig, executionContext: ExecutionContext) extends StrictLogging {
  implicit val ec = executionContext
  val uri = config.uri

  def listMetricNames: Future[Seq[MetricName]] = {
    wsClient
      .url(uri / "api" / "v1" / "metricnames")
      .applyKairosDBAuth
      .get
      .map { res =>
        (res.json \ "results").validate[Seq[String]].asOpt.toSeq.flatten.map(MetricName)
      }
  }

  def listTagNames: Future[Seq[String]] = {
    wsClient
      .url(uri / "api" / "v1" / "tagnames")
      .applyKairosDBAuth
      .get
      .map { res =>
        (res.json \ "results").validate[Seq[String]].asOpt.toSeq.flatten
      }
  }

  def listTagValues: Future[Seq[String]] = {
    wsClient
      .url(uri / "api" / "v1" / "tagvalues")
      .applyKairosDBAuth
      .get
      .map { res =>
        (res.json \ "results").validate[Seq[String]].asOpt.toSeq.flatten
      }
  }

  def healthStatus: Future[HealthStatusResults] = {
    wsClient
      .url(uri / "api" / "v1" / "health" / "status")
      .applyKairosDBAuth
      .get
      .map { res =>
        res.json.validate[Seq[String]].asOpt.toSeq.flatten
      }
      .map(HealthStatusResults)
  }

  def healthCheck: Future[HealthCheckResult] = {
    wsClient
      .url(uri / "api" / "v1" / "health" / "check")
      .applyKairosDBAuth
      .get
      .map(_.status)
      .map {
        case 204 => AllHealthy()
        case _   => Unhealthy()
      }
  }

  def version: Future[String] = {
    wsClient
      .url(uri / "api" / "v1" / "version")
      .applyKairosDBAuth
      .get
      .map { res =>
        (res.json \ "version").validate[String].get
      }
  }

  def deleteMetric(metricName: MetricName): Future[Unit] = {
    wsClient
      .url(uri / "api" / "v1" / "metric" / metricName.name)
      .applyKairosDBAuth
      .delete
      .map { res =>
        res.status match {
          case 204 => Unit
          case 400 => throw KairosDBResponseBadRequestException(errors = getErrors(res))
          case 500 => throw KairosDBResponseInternalServerErrorException(errors = getErrors(res))
        }
      }
  }

  def addDataPoints(dataPoints: Seq[DataPoint]): Future[Unit] = {
    val body = Json.toJson(dataPoints)

    wsClient
      .url(uri / "api" / "v1" / "datapoints")
      .applyKairosDBAuth
      .post(body)
      .map { res =>
        res.status match {
          case 204 => ()
          case 400 => throw KairosDBResponseBadRequestException(errors = getErrors(res))
          case 500 => throw KairosDBResponseInternalServerErrorException(errors = getErrors(res))
        }
      }
  }

  def addDataPoint(dataPoint: DataPoint): Future[Unit] = addDataPoints(Seq(dataPoint))

  def queryMetrics(queryMetrics: QueryMetrics): Future[Response] = {
    val query = Json.toJson(queryMetrics)

    wsClient
      .url(uri / "api" / "v1" / "datapoints" / "query")
      .applyKairosDBAuth
      .post(query)
      .map { res =>
        res.status match {
          case 200 => parseQueryResult(res.json)
          case 400 => throw KairosDBResponseBadRequestException(errors = getErrors(res))
          case 500 => throw KairosDBResponseInternalServerErrorException(errors = getErrors(res))
        }
      }
  }

  /** You can think of this as the exact same as the query but it leaves off the data and just returns the tag information.
    *
    * Note: Currently this is not implemented in the HBase datastore.
    */
  def queryMetricTags(queryMetrics: QueryMetrics): Future[TagsResponse] = {
    val query = Json.toJson(queryMetrics)

    wsClient
      .url(uri / "api" / "v1" / "datapoints" / "query" / "tags")
      .applyKairosDBAuth
      .post(query)
      .map { res =>
        res.status match {
          case 200 => res.json.validate[TagsResponse] match {
            case JsSuccess(tagsResponse, jsPath) => tagsResponse
            case JsError(errors) => throw KairosDBResponseParseException(errors = errors)
          }
          case 400 => throw KairosDBResponseBadRequestException(errors = getErrors(res))
          case 500 => throw KairosDBResponseInternalServerErrorException(errors =  getErrors(res))
        }
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

    wsClient
      .url(uri / "api" / "v1" / "datapoints" / "delete")
      .applyKairosDBAuth
      .post(query)
      .map { res =>
        res.status match {
          case 204 => ()
          case 400 => throw KairosDBResponseBadRequestException(errors = getErrors(res))
          case 500 => throw KairosDBResponseInternalServerErrorException(errors = getErrors(res))
        }
      }
  }

  private def parseQueryResult(json: JsValue): Response = {
    json.validate[Response] match {
      case JsSuccess(res, jsPath) => res
      case JsError(errors) => throw KairosDBResponseParseException(errors = errors)
    }
  }

  private def getErrors(res: WSResponse): Seq[String] = (res.json \ "errors").validate[Seq[String]].asOpt.toSeq.flatten

  private implicit class PimpedWSRequest(req: WSRequest) {
    def applyKairosDBAuth: WSRequest = {
      (for {
        user <- uri.user
        pass <-  uri.password
      } yield req.withAuth(user, pass, WSAuthScheme.BASIC)) getOrElse req
    }
  }
}
package io.waylay.kairosdb.driver

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

import io.waylay.kairosdb.driver.models._
import io.waylay.kairosdb.driver.models.HealthCheckResult._
import com.netaporter.uri.dsl._
import com.typesafe.scalalogging.StrictLogging
import io.waylay.kairosdb.driver.KairosDB._
import io.waylay.kairosdb.driver.models.QueryMetricTagsResponse.TagsResponse
import io.waylay.kairosdb.driver.models.json.Formats._
import io.waylay.kairosdb.driver.models.QueryResponse.Response
import play.api.libs.json._
import play.api.libs.ws.{StandaloneWSClient, StandaloneWSRequest, StandaloneWSResponse, WSAuthScheme}
import play.api.libs.ws.DefaultBodyWritables._
import play.api.libs.ws.JsonBodyWritables._
import play.api.libs.ws.JsonBodyReadables._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

object KairosDB {

  /** Parent exception for all exceptions this driver may throw */
  class KairosDBException(msg: String) extends Exception(msg)

  /** Thrown when the response by KairosDB can't be parsed */
  case class KairosDBResponseParseException(msg: String = "Could not parse KairosDB response", errors: Seq[(JsPath, Seq[JsonValidationError])])
    extends KairosDBException(s"""$msg (errors: ${errors.mkString(", ")})""")

  case class KairosDBResponseException(httpErrorCode:Int, httpStatusText:String, errors: Seq[String])
    extends KairosDBException(s"""KairosDB returned HTTP $httpErrorCode ($httpStatusText) (errors: ${errors.mkString(", ")})""")
}

class KairosDB(wsClient: StandaloneWSClient, config: KairosDBConfig, executionContext: ExecutionContext) extends StrictLogging {
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

  def addDataPoints(dataPoints: Seq[DataPoint], gzip: Boolean = false): Future[Unit] = {
    val body = Json.toJson(dataPoints)
    logger.debug(Json.prettyPrint(body))

    val (actualBody, contentType) = if(gzip){
      val buff = new ByteArrayOutputStream()
      val gzos = new GZIPOutputStream(buff)
      gzos.write(Json.toBytes(body))
      gzos.finish()
      val gzipBody = buff.toByteArray
      (gzipBody, "application/gzip")
    }else{
      (Json.toBytes(body), "application/json")
    }

    wsClient
      .url(uri / "api" / "v1" / "datapoints")
      .applyKairosDBAuth
      .addHttpHeaders("Content-Type" -> contentType)
      .post(actualBody)
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

  private def getErrors(res: StandaloneWSResponse): Seq[String] = {
    Try(res.body[JsValue]) match {
      case Success(json) =>
        (json \ "errors").validate[Seq[String]].asOpt.toSeq.flatten
      case Failure(_) =>
        // not a json response like jetty default 401
        Seq.empty
    }

  }

  private implicit class PimpedWSRequest(req: StandaloneWSRequest) {
    def applyKairosDBAuth: StandaloneWSRequest = {
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

  private def emptyHandling : PartialFunction[StandaloneWSResponse, Unit] = {
    case res if res.status == 204 => ()
  }

  private def withBodyHandling[T](transform: JsValue => JsResult[T], successStatusCode: Int): PartialFunction[StandaloneWSResponse, T] = {
    case res if res.status == successStatusCode =>
      Try(res.body[JsValue]) match {
        case Success(json) =>
          transform(json) match {
            case JsSuccess(result, jsPath) => result
            case JsError(errors) => throw KairosDBResponseParseException(errors = errors)
          }
        case Failure(e) =>
          throw KairosDBResponseParseException("Failed to parse response body to json", Seq())
      }
  }

  private def responseErrorHandling[T](res: StandaloneWSResponse) = {
    throw KairosDBResponseException(res.status, res.statusText, getErrors(res))
  }

}

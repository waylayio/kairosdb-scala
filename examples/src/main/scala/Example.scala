import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.Implicits._
import io.waylay.kairosdb.driver.models.KairosCompatibleType.KNumber
import io.waylay.kairosdb.driver.models.KairosQuery.QueryTag
import io.waylay.kairosdb.driver.models.TimeSpan.RelativeStartTime
import io.waylay.kairosdb.driver.models._
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

object Example extends App {

  implicit val actorSystem = ActorSystem()
  implicit val actorMaterializer = ActorMaterializer()
  val wsClient = AhcWSClient()
  val kairosDB = new KairosDB(wsClient, KairosDBConfig(), global)

  val res = for {
    version <- kairosDB.version
    names <- kairosDB.listMetricNames
    _ <- kairosDB.addDataPoint(DataPoint("kairosdbscala.test", 9001, tags = Tag("awesome", "yes")))

    // same as above, but without implicit conversions
    _ <- kairosDB.addDataPoint(DataPoint(MetricName("kairosdbscala.test"), KNumber(9001), tags = Seq(Tag("awesome", "yes"))))

    qr <- kairosDB.queryMetrics(
      QueryMetrics(
        Query("kairosscala.test", tags = QueryTag("awesome" -> "yes")), 5.minutes.ago.startTime
      )
    )

    // same as above, but without implicits
    _ <- kairosDB.queryMetrics(
      QueryMetrics(Seq(
        Query(MetricName("kairosscala.test"), tags = Seq(QueryTag("awesome", Seq("yes", "true"))))
      ), TimeSpan(RelativeStartTime(5.minutes)))
    )
  } yield {

    println(s"The KairosDB version is $version.")
    println(s"""Some of the metrics are ${names take 3 map (_.name) mkString ", "}.""")
    println(s"The result of querying was ${qr.queries.head.results.head}.")
  }

  res.onComplete { _ =>
    Try(wsClient.close())
    Try(actorMaterializer.shutdown())
    Try(actorSystem.terminate())
  }

  res.onComplete{
    case Success(_) => println("done")
    case Failure(e) => e.printStackTrace()
  }
}

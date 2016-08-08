import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws.ahc.AhcWSClient
import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.Implicits._
import io.waylay.kairosdb.driver.models.KairosCompatibleType.KNumber
import io.waylay.kairosdb.driver.models.KairosQuery.QueryTag
import io.waylay.kairosdb.driver.models.TimeSpan.RelativeStartTime
import io.waylay.kairosdb.driver.models._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Example extends App {

  implicit val actorSystem = ActorSystem()
  implicit val actorMaterializer = ActorMaterializer()
  val wsClient = AhcWSClient()
  val kairosDB = new KairosDB(wsClient, KairosDBConfig(), global)

  for {
    version <- kairosDB.version
    names <- kairosDB.listMetricNames
    _ <- kairosDB.addDataPoint(DataPoint("kairosdbscala.test", 9001, tags = Tag("awesome", "yes")))

    // same as above, but without implicit conversions
    _ <- kairosDB.addDataPoint(DataPoint(MetricName("kairosdbscala.test"), KNumber(9001), tags = Seq(Tag("awesome", "yes"))))

    qr <- kairosDB.queryMetrics(
      QueryMetrics(
        Query("kairosscala.test", tags = QueryTag("awesome", "yes")), 5.minutes.ago.startTime
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

    wsClient.close()
    actorMaterializer.shutdown()
    actorSystem.terminate()
  }
}

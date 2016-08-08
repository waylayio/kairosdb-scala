package integration

import java.time.Instant

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.spotify.docker.client.DefaultDockerClient
import com.typesafe.scalalogging.StrictLogging
import com.whisk.docker.DockerFactory
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import io.waylay.kairosdb.driver.Implicits._
import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.models.KairosCompatibleType.KNumber
import io.waylay.kairosdb.driver.models.KairosQuery.QueryTag
import io.waylay.kairosdb.driver.models.QueryResponse.{ResponseQuery, Result, TagResult}
import io.waylay.kairosdb.driver.models._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Second, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.ExecutionContext.global

class AddAndQueryDataPointsIntegrationSpec extends FlatSpec with Matchers with ScalaFutures with StrictLogging with BeforeAndAfterAll
  with DockerKairosDBService
  with DockerTestKit {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))
  override def dockerInitPatienceInterval = PatienceConfig(scaled(Span(30, Seconds)), scaled(Span(10, Millis)))

  override implicit val dockerFactory: DockerFactory = new SpotifyDockerFactory(DefaultDockerClient.fromEnv().build())

  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()
  val wsClient = AhcWSClient()

  override def afterAll(): Unit = {
    wsClient.close()
    materializer.shutdown()
    actorSystem.terminate()
    super.afterAll()
  }

  "Adding data points and then querying them" should "work for a single data point" in {
    val instant = Instant.ofEpochSecond(1470837457L)
    val start = Instant.ofEpochSecond(1470830000L)
    val qm = QueryMetrics(Seq(Query("my.new.metric", QueryTag("aoeu" -> "snth"))), start)

    val res = kairosdbContainer.getPorts().map(_ (DefaultKairosDbPort)).flatMap { kairosPort: Int =>
      val kairosDB = new KairosDB(wsClient, KairosDBConfig(port = kairosPort), global)

      kairosDB.addDataPoint(DataPoint(MetricName("my.new.metric"), KNumber(555), instant, Seq(Tag("aoeu", "snth")))) flatMap { _ =>
        kairosDB.queryMetrics(qm)
      }
    }.futureValue

    res should be(QueryResponse.Response(Seq(ResponseQuery(1, Seq(
      Result("my.new.metric", Seq(GroupBy.GroupByType("number")), Seq(TagResult("aoeu", Seq("snth"))), Seq( (instant, KNumber(555)) ) )
    )))))
  }
}

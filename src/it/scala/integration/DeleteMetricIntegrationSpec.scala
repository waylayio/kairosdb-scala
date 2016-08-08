package integration

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.spotify.docker.client.DefaultDockerClient
import com.typesafe.scalalogging.StrictLogging
import com.whisk.docker.DockerFactory
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.models.KairosCompatibleType.KNumber
import io.waylay.kairosdb.driver.models.{DataPoint, KairosDBConfig, MetricName, Tag}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.ExecutionContext.global

class DeleteMetricIntegrationSpec extends FlatSpec with Matchers with ScalaFutures with StrictLogging with BeforeAndAfterAll
  with DockerKairosDBService
  with DockerTestKit {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

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

  "Deleting a metric name" should "after deleting a metric, all metrics should not contain the metric" in {
    val res = kairosdbContainer.getPorts().map(_ (DefaultKairosDbPort)).flatMap { kairosPort: Int =>
      val kairosDB = new KairosDB(wsClient, KairosDBConfig(port = kairosPort), global)
      kairosDB.addDataPoint(DataPoint(MetricName("my.new.metric"), KNumber(555), tags = Seq(Tag("aoeu", "snth")))) flatMap { _ =>
        kairosDB.deleteMetric(MetricName("my.new.metric"))
      } flatMap { _ =>
        kairosDB.listMetricNames
      }
    }.futureValue

    res should not contain MetricName("my.new.metric")
  }
}

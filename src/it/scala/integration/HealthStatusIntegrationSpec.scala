package integration

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.spotify.docker.client.DefaultDockerClient
import com.typesafe.scalalogging.StrictLogging
import com.whisk.docker.DockerFactory
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.models.HealthCheckResult.AllHealthy
import io.waylay.kairosdb.driver.models._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.ExecutionContext.global

class HealthStatusIntegrationSpec extends FlatSpec with Matchers with ScalaFutures with StrictLogging with BeforeAndAfterAll
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

  "The health check" should "return all healthy" in {
    val res = kairosdbContainer.getPorts().map(_ (DefaultKairosDbPort)).flatMap { kairosPort: Int =>
      val kairosDB = new KairosDB(wsClient, KairosDBConfig(port = kairosPort), global)
      kairosDB.healthCheck
    }.futureValue

    res should be(AllHealthy())
  }

  "The health status" should "respond that there are no thread deadlocks and datastore query works" in {
    val res = kairosdbContainer.getPorts().map(_ (DefaultKairosDbPort)).flatMap { kairosPort: Int =>
      val kairosDB = new KairosDB(wsClient, KairosDBConfig(port = kairosPort), global)
      kairosDB.healthStatus
    }.futureValue

    res should be(HealthStatusResults(Seq("JVM-Thread-Deadlock: OK", "Datastore-Query: OK")))
  }
}

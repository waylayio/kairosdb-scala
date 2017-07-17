package integration

import com.spotify.docker.client.DefaultDockerClient
import com.typesafe.scalalogging.StrictLogging
import com.whisk.docker.DockerFactory
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Second, Seconds, Span}
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.api.test.NoMaterializer

import scala.concurrent.duration._

trait IntegrationSpec extends FlatSpec with Matchers with ScalaFutures with StrictLogging with BeforeAndAfterAll
  with DockerKairosDBService
  with DockerTestKit {

  override val StartContainersTimeout = 30.seconds

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))
  override def dockerInitPatienceInterval = PatienceConfig(scaled(Span(30, Seconds)), scaled(Span(10, Millis)))

  override implicit val dockerFactory: DockerFactory = new SpotifyDockerFactory(DefaultDockerClient.fromEnv().build())

  implicit val materializer = NoMaterializer
  val wsClient = StandaloneAhcWSClient()

  override def afterAll(): Unit = {
    wsClient.close()
    super.afterAll()
  }
}

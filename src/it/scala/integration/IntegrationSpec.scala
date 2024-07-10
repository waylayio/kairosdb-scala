package integration

import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService, ForAllTestContainer}
import com.typesafe.scalalogging.StrictLogging
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.testkit.NoMaterializer
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.testcontainers.containers.wait.strategy.Wait
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import java.io.File

trait IntegrationSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with StrictLogging
    with BeforeAndAfterAll
    with ForAllTestContainer {

  lazy val DefaultKairosDbPort = 8080

  override val container = DockerComposeContainer(
    new File("src/it/resources/docker-compose.yaml"),
    tailChildContainers = true,
    exposedServices =
      Seq(ExposedService("kairosdb", 8080, Wait.forHttp("/api/v1/version").withBasicCredentials("test", "test")))
  )

  lazy val kairosPort: Int =
    container.getServicePort("kairosdb", 8080)

  implicit val pc: PatienceConfig = PatienceConfig(Span(2000, Seconds), Span(1, Second))
  // override def dockerInitPatienceInterval = PatienceConfig(scaled(Span(30, Seconds)), scaled(Span(10, Millis)))

  implicit val materializer: Materializer = NoMaterializer
  val wsClient: StandaloneAhcWSClient     = StandaloneAhcWSClient()

  override def afterAll(): Unit = {
    wsClient.close()
    super.afterAll()
  }
}

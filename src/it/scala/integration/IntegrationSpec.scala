package integration

import akka.stream.testkit.NoMaterializer
import com.spotify.docker.client.messages.HostConfig
import com.typesafe.scalalogging.StrictLogging
import com.whisk.docker.testkit.{ContainerGroup, ContainerSpec, DockerReadyChecker}
import com.whisk.docker.testkit.scalatest.DockerTestKitForAll
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.ws.ahc.StandaloneAhcWSClient

trait IntegrationSpec extends AnyWordSpec with Matchers with ScalaFutures with StrictLogging with BeforeAndAfterAll with DockerTestKitForAll {

  lazy val DefaultKairosDbPort = 8080

  lazy val env = Seq.empty[String]
  lazy val volumes = Seq.empty[HostConfig.Bind]

  val kairosdbContainer = ContainerSpec("brunoballekens/kairosdb-scala-driver-it:1.2.0-1")
    .withEnv(env:_*)
    // broken with the spotify client
    .withVolumeBindings(volumes:_*)
    .withExposedPorts(DefaultKairosDbPort)
    //      .withReadyChecker(
    //        DockerReadyChecker.HttpResponseCode(DefaultKairosDbPort, "/api/v1/version", code = 200)
    //          .within(100.millis)
    //          .looped(20, 250.millis))
    .withReadyChecker(DockerReadyChecker.LogLineContains("KairosDB service started"))
    //.withReadyChecker(LoggingLogLineContains("KairosDB service started"))

  lazy val kairosPort: Int = {
    managedContainers.containers.head
      .mappedPortOpt(DefaultKairosDbPort)
      .getOrElse(throw new IllegalStateException(s"Missing container mapped port for $DefaultKairosDbPort"))
  }

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))
  //override def dockerInitPatienceInterval = PatienceConfig(scaled(Span(30, Seconds)), scaled(Span(10, Millis)))

  override val managedContainers: ContainerGroup = ContainerGroup(Seq(kairosdbContainer.toContainer))

  implicit val materializer = NoMaterializer
  val wsClient = StandaloneAhcWSClient()

  override def afterAll(): Unit = {
    wsClient.close()
    super.afterAll()
  }
}

//private case class LoggingLogLineContains(str: String) extends DockerReadyChecker with StrictLogging {
//
//  override def apply(container: BaseContainer)(implicit docker: ContainerCommandExecutor, ec: ExecutionContext): Future[Unit] = {
//    container.state() match {
//      case ContainerState.Ready(_) =>
//        Future.successful(())
//      case state: ContainerState.HasId =>
//        docker.withLogStreamLinesRequirement(state.id, withErr = true) { m =>
//          // drop newlines
//          logger.info(m.dropRight(1))
//          m.contains(str)
//        }.map(_ => ())
//      case _ =>
//        Future.failed(
//          new FailFastCheckException("can't initialise LogStream to container without Id")
//        )
//    }
//  }
//}

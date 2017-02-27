package integration

import com.typesafe.scalalogging.StrictLogging
import com.whisk.docker._

import scala.concurrent.{ExecutionContext, Future}

trait DockerKairosDBService extends DockerKit {
  lazy val DefaultKairosDbPort = 8080

  lazy val env = Seq.empty[String]
  lazy val volumes = Seq.empty[VolumeMapping]

  val kairosdbContainer = DockerContainer("thomastoye/kairosdb-scala-driver-it:latest")
    .withEnv(env:_*)
    // broken with the spotify client
    .withVolumes(volumes)
    .withPorts(DefaultKairosDbPort -> None)
//      .withReadyChecker(
//        DockerReadyChecker.HttpResponseCode(DefaultKairosDbPort, "/api/v1/version", code = 200)
//          .within(100.millis)
//          .looped(20, 250.millis))
  .withReadyChecker(DockerReadyChecker.LogLineContains("KairosDB service started"))
//  .withReadyChecker(LoggingLogLineContains("KairosDB service started"))

  abstract override def dockerContainers: List[DockerContainer] = kairosdbContainer :: super.dockerContainers

  lazy val kairosPort = kairosdbContainer.getPorts().map(_ (DefaultKairosDbPort))

}

private case class LoggingLogLineContains(str: String) extends DockerReadyChecker with StrictLogging{

  override def apply(container: DockerContainerState)(implicit docker: DockerCommandExecutor, ec: ExecutionContext): Future[Boolean] = {
    container.id.map{id =>
      docker.withLogStreamLines(id, withErr = true){m =>
        // drop newlines
        logger.info(m.dropRight(1))
        m.contains(str)
      }
      true
    }
  }
}

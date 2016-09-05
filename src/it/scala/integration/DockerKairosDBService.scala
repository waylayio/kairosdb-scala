package integration

import com.whisk.docker._

import scala.concurrent.{ExecutionContext, Future}

trait DockerKairosDBService extends DockerKit {
  lazy val DefaultKairosDbPort = 8080

  val kairosdbContainer = DockerContainer("thomastoye/kairosdb-scala-driver-it:latest")
    .withPorts(DefaultKairosDbPort -> None)
//      .withReadyChecker(
//        DockerReadyChecker.HttpResponseCode(DefaultKairosDbPort, "/api/v1/version", code = 200)
//          .within(100.millis)
//          .looped(20, 250.millis))
  .withReadyChecker(DockerReadyChecker.LogLineContains("KairosDB service started"))
//  .withReadyChecker(LoggingLogLineContains("KairosDB service started"))

  abstract override def dockerContainers: List[DockerContainer] = kairosdbContainer :: super.dockerContainers


}

private case class LoggingLogLineContains(str: String) extends DockerReadyChecker {
  override def apply(container: DockerContainerState)(implicit docker: DockerCommandExecutor, ec: ExecutionContext): Future[Boolean] = {
    for {
      id <- container.id
      _ <- docker.withLogStreamLines(id, withErr = true){m =>
        println(m)
        m.contains(str)
      }
    } yield {
      true
    }
  }
}

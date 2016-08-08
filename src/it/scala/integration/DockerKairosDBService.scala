package integration

import com.whisk.docker.{DockerContainer, DockerKit, DockerReadyChecker}

trait DockerKairosDBService extends DockerKit {
  lazy val DefaultKairosDbPort = 8080

  val kairosdbContainer = DockerContainer("thomastoye/kairosdb-scala-driver-it:latest")
    .withPorts(DefaultKairosDbPort -> None)
    .withReadyChecker(DockerReadyChecker.LogLineContains("KairosDB service started"))

  abstract override def dockerContainers: List[DockerContainer] = kairosdbContainer :: super.dockerContainers
}

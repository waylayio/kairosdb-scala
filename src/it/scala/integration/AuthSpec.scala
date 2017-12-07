package integration

import java.nio.file.Paths

import com.whisk.docker.VolumeMapping
import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.KairosDB.KairosDBResponseException
import io.waylay.kairosdb.driver.models._

import scala.concurrent.ExecutionContext.global

class AuthSpec extends IntegrationSpec {

  // TODO enabling auth by providing env variables, when kairosdb 1.1.2 is released
  //  // same as KairosDB Main.java
  //  def toEnvVarName(propName: String) = {
  //    propName.toUpperCase().replace('.', '_')
  //  }
  //
  //  override lazy val dockerEnv = Seq(
  //    toEnvVarName("kairosdb.jetty.basic_auth.user") + "=test",
  //    toEnvVarName("kairosdb.jetty.basic_auth.password") + "=test"
  //  )

  // enabling auth by providing a properties file
  override lazy val volumes = Seq(
    VolumeMapping(Paths.get("src/it/resources/conf").toAbsolutePath.toString, "/opt/kairosdb/conf")
  )

  "The health status" should " fail without auth" in {
    val res = kairosPort.flatMap { kairosPort =>
      val kairosDB = new KairosDB(wsClient, KairosDBConfig(port = kairosPort), global)
      kairosDB.healthStatus
    }.failed.futureValue

    res shouldBe an[KairosDBResponseException]
    res should be(KairosDBResponseException(401, "Unauthorized", Seq.empty))
  }

  it should "succeed with auth" in {

    val res = kairosPort.flatMap { kairosPort =>
      val kairosConfig = KairosDBConfig(
        port = kairosPort,
        username = Some("test"),
        password = Some("test")
      )
      val kairosDB = new KairosDB(wsClient, kairosConfig, global)
      kairosDB.healthStatus
    }.futureValue

    res should be(HealthStatusResults(Seq("JVM-Thread-Deadlock: OK", "Datastore-Query: OK")))
  }
}

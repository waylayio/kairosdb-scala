import java.time.Instant

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws.ahc.AhcWSClient
import io.waylay.kairosdb.driver.Implicits._
import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.models._
import io.waylay.kairosdb.driver.models.KairosCompatibleType._
import io.waylay.kairosdb.driver.models.KairosQuery._
import io.waylay.kairosdb.driver.models.TimeSpan._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

implicit val actorSystem = ActorSystem()
implicit val actorMaterializer = ActorMaterializer()
val wsClient = AhcWSClient()
val kairosDB = new KairosDB(wsClient, KairosDBConfig(), global)

def closeAll = {
  wsClient.close()
  actorMaterializer.shutdown()
  actorSystem.terminate()
}

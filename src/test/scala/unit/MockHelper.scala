package unit

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.{ActorMaterializer, Materializer}
import org.specs2.specification.AfterAll
import play.api.mvc.{DefaultActionBuilder, PlayBodyParsers}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * See https://github.com/leanovate/play-mockws/issues/29
  */
trait MockHelper extends AfterAll{
  private implicit val sys: ActorSystem = ActorSystem("test")
  private implicit val mat:Materializer = ActorMaterializer()

  val BodyParser: PlayBodyParsers = PlayBodyParsers()
  val Action: DefaultActionBuilder = DefaultActionBuilder(BodyParser.anyContent)(mat.executionContext)

  override def afterAll() = {
    mat.shutdown()
    Await.result(sys.terminate(), 10.seconds)
  }
}

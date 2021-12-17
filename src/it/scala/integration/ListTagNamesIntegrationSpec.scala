package integration

import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.models.KairosCompatibleType.KNumber
import io.waylay.kairosdb.driver.models.{DataPoint, KairosDBConfig, MetricName, Tag}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.immutable.Seq

class ListTagNamesIntegrationSpec extends IntegrationSpec {

  "Listing tag names" should {

    "only return tag of point that was just inserted" in {

      val kairosDB = new KairosDB(wsClient, KairosDBConfig(port = kairosPort), global)
      val res = kairosDB.addDataPoint(DataPoint(MetricName("my.new.metric"), KNumber(555), tags = Seq(Tag("aoeu", "snth")))).flatMap { _ =>
        kairosDB.listTagNames
      }.futureValue

      // these appear automatically after a while
      val withSystemtagsRemoved = res.toSet -- Set("method", "host")

      withSystemtagsRemoved must be(Set("aoeu"))
    }
  }
}

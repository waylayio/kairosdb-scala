package integration

import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.models.KairosCompatibleType.KNumber
import io.waylay.kairosdb.driver.models.{DataPoint, KairosDBConfig, MetricName, Tag}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.immutable.Seq

class ListMetricsIntegrationSpec extends IntegrationSpec {

  "Listing metric names" should {

    "after inserting a new datapoint into a metric, all metrics should contain this metric" in {
      val kairosDB = new KairosDB(wsClient, KairosDBConfig(port = kairosPort), global)
      val res = kairosDB.addDataPoint(DataPoint(MetricName("my.new.metric"), KNumber(555), tags = Seq(Tag("aoeu", "snth")))).flatMap { _ =>
        kairosDB.listMetricNames
      }.futureValue

      res must contain(MetricName("my.new.metric"))
    }
  }
}

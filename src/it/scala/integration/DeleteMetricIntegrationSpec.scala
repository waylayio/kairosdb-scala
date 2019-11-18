package integration

import io.waylay.kairosdb.driver.KairosDB
import io.waylay.kairosdb.driver.models.KairosCompatibleType.KNumber
import io.waylay.kairosdb.driver.models.{DataPoint, KairosDBConfig, MetricName, Tag}

import scala.concurrent.ExecutionContext.Implicits.global

import scala.collection.immutable.Seq

class DeleteMetricIntegrationSpec extends IntegrationSpec {
  "Deleting a metric name" should "after deleting a metric, all metrics should not contain the metric" in {
    val kairosDB = new KairosDB(wsClient, KairosDBConfig(port = kairosPort), global)
    val res = kairosDB.addDataPoint(DataPoint(MetricName("my.new.metric"), KNumber(555), tags = Seq(Tag("aoeu", "snth")))).flatMap{ _ =>
      kairosDB.deleteMetric(MetricName("my.new.metric"))
    }.flatMap{ _ =>
      kairosDB.listMetricNames
    }.futureValue

    res should not contain MetricName("my.new.metric")
  }
}

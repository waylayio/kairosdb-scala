package unit

import java.time.Instant
import java.util.concurrent.TimeUnit

import io.waylay.kairosdb.driver.models.Aggregator._
import io.waylay.kairosdb.driver.models.GroupBy._
import io.waylay.kairosdb.driver.models.KairosQuery.Order.Descending
import io.waylay.kairosdb.driver.models.KairosQuery.{Order, QueryTag}
import io.waylay.kairosdb.driver.models.TimeSpan._
import io.waylay.kairosdb.driver.models.json.Formats._
import io.waylay.kairosdb.driver.models._
import org.specs2.mutable.Specification
import play.api.libs.json.Json

import scala.concurrent.duration._

class QueryWritesSpec extends Specification {
  "Kairos query builder" should {
    "Correctly serialize a minimal query" in {
      val query = Query(MetricName("mymetric"))
      Json.toJson(query) must be equalTo Json.obj("name" -> "mymetric")
    }

    "Correctly serialize a query with descending order" in {
      val query = Query(MetricName("mymetric"), order = Order.Descending)
      Json.toJson(query) must be equalTo Json.obj("name" -> "mymetric", "order" -> "desc")
    }

    "Correctly serialize a query with default order" in {
      val query = Query(MetricName("mymetric"), order = Order.defaultOrder)
      Json.toJson(query) must be equalTo Json.obj("name" -> "mymetric")
    }

    "Correctly serialize a query with limit" in {
      val query = Query(MetricName("mymetric"), limit = Some(168))
      Json.toJson(query) must be equalTo Json.obj("name" -> "mymetric", "limit" -> 168)
    }

    "Correctly serialize a query with tags" in {
      val query = Query(MetricName("mymetric"), tags = Seq(QueryTag("host", Seq("foo1", "foo2")), QueryTag("customer", Seq("bar"))))
      Json.toJson(query) must be equalTo Json.obj(
        "name" -> "mymetric",
        "tags" -> Json.obj("host" -> Seq("foo1", "foo2"), "customer" -> Seq("bar"))
      )
    }

    "Correctly serialize a query with a simple aggregator" in {
      val query = Query(MetricName("mymetric"), aggregators = Seq(Diff()))
      Json.toJson(query) must be equalTo Json.obj("name" -> "mymetric", "aggregators" -> Seq(Json.obj("name" -> "diff")))
    }

    "Correctly serialize a query with a complex aggregator" in {
      val aggJson = Json.obj(
        "name" -> "rate",
        "unit" -> "seconds",
        "sampling" -> Json.obj(
          "unit" -> "days",
          "value" -> "2"
        ),
        "time_zone" -> "America/Chihuahua"
      )

      val query = Query(MetricName("mymetric"), aggregators = Seq(Rate(TimeUnit.SECONDS, 2.days, Some("America/Chihuahua"))))
      Json.toJson(query) must be equalTo Json.obj("name" -> "mymetric", "aggregators" -> Seq(aggJson))
    }

    "Correctly serialize a query with multiple aggregators" in {
      val aggJson = Json.obj(
        "name" -> "rate",
        "unit" -> "seconds",
        "sampling" -> Json.obj(
          "unit" -> "days",
          "value" -> "2"
        ),
        "time_zone" -> "America/Chihuahua"
      )

      val query = Query(
        MetricName("mymetric"),
        aggregators = Seq(Rate(TimeUnit.SECONDS, 2.days, Some("America/Chihuahua")), Diff(), Scale(10.7))
      )

      Json.toJson(query) must be equalTo Json.obj(
        "name" -> "mymetric",
        "aggregators" -> Seq(aggJson, Json.obj("name" -> "diff"), Json.obj("name" -> "scale", "factor" -> 10.7))
      )
    }

    "Correctly serialize a query with a group_by" in {
      val query = Query(MetricName("anothermetric"), groupBys = Seq(GroupByTags(Seq("onetag", "twotag", "foo"))))

      Json.toJson(query) must be equalTo Json.obj("name" -> "anothermetric", "group_by" -> Seq(Json.obj("name" -> "tag", "tags" -> Seq("onetag", "twotag", "foo"))))
    }

    "Correctly serialize a query with multiple group_by's" in {
      val query = Query(MetricName("anothermetric"), groupBys = Seq(GroupByTags(Seq("onetag", "twotag", "foo")), GroupByValue(55)))

      Json.toJson(query) must be equalTo Json.obj(
        "name" -> "anothermetric",
        "group_by" -> Seq(Json.obj("name" -> "tag", "tags" -> Seq("onetag", "twotag", "foo")), Json.obj("name" -> "value", "range_size" -> 55))
      )
    }

    "Correctly serialize a query with exclude_tags" in {
      val query = Query(MetricName("mymetric"), excludeTags = true)
      Json.toJson(query) must be equalTo Json.obj("name" -> "mymetric", "exclude_tags" -> true)
    }

    "Correctly serialize a query with order" in {
      val query = Query(MetricName("mymetric"), order = Descending)
      Json.toJson(query) must be equalTo Json.obj("name" -> "mymetric", "order" -> "desc")
    }

    "Correctly serialize the first example from the KairosDB docs" in {

      val query = Query(
        MetricName("abc.123"),
        tags = Seq(QueryTag("host", Seq("foo", "foo2")), QueryTag("customer", Seq("bar"))),
        limit = Some(10000),
        aggregators = Seq(Sum(10.minutes))
      )

      Json.toJson(query) must be equalTo Json.parse(
        """
          |{
          |  "tags": {
          |    "host": ["foo", "foo2"],
          |    "customer": ["bar"]
          |  },
          |  "name": "abc.123",
          |  "limit": 10000,
          |  "aggregators": [
          |    {
          |      "name": "sum",
          |      "sampling": {
          |        "value": "10",
          |        "unit": "minutes"
          |      }
          |    }
          |  ]
          |}
        """.stripMargin)
    }

    "Correctly serialize the second example from the KairosDB docs" in {

      val query = Query(
        MetricName("xyz.123"),
        tags = Seq(QueryTag("host", Seq("foo", "foo2")), QueryTag("customer", Seq("bar"))),
        aggregators = Seq(Average(10.minutes))
      )

      Json.toJson(query) must be equalTo Json.parse(
        """
          |{
          |  "tags": {
          |    "host": ["foo", "foo2"],
          |    "customer": ["bar"]
          |  },
          |  "name": "xyz.123",
          |  "aggregators": [
          |  {
          |    "name": "avg",
          |    "sampling": {
          |      "value": "10",
          |      "unit": "minutes"
          |    }
          |  }
          |  ]
          |}
        """.stripMargin)
    }
  }

  "Kairos query metrics builder" should {
    "Correctly serialize absolute start times" in {
      val qm = QueryMetrics(Seq(), TimeSpan(AbsoluteStartTime(Instant.ofEpochSecond(1470052425L))))

      Json.toJson(qm) should be equalTo Json.obj("start_absolute" -> 1470052425000L, "metrics" -> Seq.empty[String])
    }

    "Correctly serialize relative start times" in {
      val qm = QueryMetrics(Seq(), TimeSpan(RelativeStartTime(4.days)))

      Json.toJson(qm) should be equalTo Json.obj(
        "start_relative" -> Json.obj("unit" -> "days", "value" -> "4"),
        "metrics" -> Seq.empty[String]
      )
    }

    "Correctly serialize absolute start times with absolute end times" in {
      val qm = QueryMetrics(Seq(), TimeSpan(AbsoluteStartTime(Instant.ofEpochSecond(1470052425L)), Some(AbsoluteEndTime(Instant.ofEpochSecond(1470052997L)))))

      Json.toJson(qm) should be equalTo Json.obj(
        "start_absolute" -> 1470052425000L,
        "end_absolute" -> 1470052997000L,
        "metrics" -> Seq.empty[String]
      )
    }

    "Correctly serialize absolute start times with relative end times" in {
      val qm = QueryMetrics(Seq(), TimeSpan(AbsoluteStartTime(Instant.ofEpochSecond(1470052425L)), Some(RelativeEndTime(14.seconds))))

      Json.toJson(qm) should be equalTo Json.obj(
        "start_absolute" -> 1470052425000L,
        "end_relative" -> Json.obj("unit" -> "seconds", "value" -> "14"),
        "metrics" -> Seq.empty[String]
      )
    }

    "Correctly serialize relative start times with absolute end time" in {
      val qm = QueryMetrics(Seq(), TimeSpan(RelativeStartTime(4.days)))

      Json.toJson(qm) should be equalTo Json.obj(
        "start_relative" -> Json.obj("unit" -> "days", "value" -> "4"),
        "metrics" -> Seq.empty[String]
      )
    }

    "Correctly serialize relative start times with relative end time" in {
      val qm = QueryMetrics(Seq(), TimeSpan(RelativeStartTime(4.days), Some(RelativeEndTime(7.minutes))))

      Json.toJson(qm) should be equalTo Json.obj(
        "start_relative" -> Json.obj("unit" -> "days", "value" -> "4"),
        "end_relative" -> Json.obj("unit" -> "minutes", "value" -> "7"),
        "metrics" -> Seq.empty[String]
      )
    }

    "Correctly serialize time zones" in {
      val qm = QueryMetrics(Seq(), TimeSpan(AbsoluteStartTime(Instant.ofEpochSecond(1470052425L))), timeZone = Some("America/Indiana/Indianapolis"))

      Json.toJson(qm) should be equalTo Json.obj(
        "start_absolute" -> 1470052425000L,
        "metrics" -> Seq.empty[String],
        "time_zone" -> "America/Indiana/Indianapolis"
      )
    }

    "Correctly serialize cache time 700" in {
      val qm = QueryMetrics(Seq(), TimeSpan(AbsoluteStartTime(Instant.ofEpochSecond(1470052425L))), cacheTime = Some(700))

      Json.toJson(qm) should be equalTo Json.obj(
        "start_absolute" -> 1470052425000L,
        "metrics" -> Seq.empty[String],
        "cache_time" -> 700
      )
    }

    "Correctly serialize cache time 0" in {
      val qm = QueryMetrics(Seq(), TimeSpan(AbsoluteStartTime(Instant.ofEpochSecond(1470052425L))), cacheTime = Some(0))

      Json.toJson(qm) should be equalTo Json.obj(
        "start_absolute" -> 1470052425000L,
        "metrics" -> Seq.empty[String],
        "cache_time" -> 0
      )
    }

    "Correctly serialize with a query" in {
      val query = Query(MetricName("mymetric"), tags = Seq(QueryTag("host", Seq("foo1", "foo2")), QueryTag("customer", Seq("bar"))))
      val qm = QueryMetrics(Seq(query), TimeSpan(AbsoluteStartTime(Instant.ofEpochSecond(1470052425L))), cacheTime = Some(0))

      Json.toJson(qm) should be equalTo Json.obj(
        "start_absolute" -> 1470052425000L,
        "metrics" -> Seq(
          Json.obj(
            "name" -> "mymetric",
            "tags" -> Json.obj("host" -> Seq("foo1", "foo2"), "customer" -> Seq("bar"))
          )
        ),
        "cache_time" -> 0
      )
    }

    "Correctly serialize query from the KairosDB docs" in {
      val query1 = Query(
        MetricName("abc.123"),
        tags = Seq(QueryTag("host", Seq("foo", "foo2")), QueryTag("customer", Seq("bar"))),
        limit = Some(10000),
        aggregators = Seq(Sum(10.minutes))
      )

      val query2 = Query(
        MetricName("xyz.123"),
        tags = Seq(QueryTag("host", Seq("foo", "foo2")), QueryTag("customer", Seq("bar"))),
        aggregators = Seq(Average(10.minutes))
      )

      val qm = QueryMetrics(
        Seq(query1, query2),
        TimeSpan(AbsoluteStartTime(Instant.ofEpochMilli(1357023600000L)), Some(RelativeEndTime(5.days))),
        timeZone = Some("Asia/Kabul")
      )

      Json.toJson(qm) must be equalTo Json.parse(
        """
          |{
          |   "start_absolute": 1357023600000,
          |   "end_relative": {
          |       "value": "5",
          |       "unit": "days"
          |   },
          |   "time_zone": "Asia/Kabul",
          |   "metrics": [
          |       {
          |           "tags": {
          |               "host": ["foo", "foo2"],
          |               "customer": ["bar"]
          |           },
          |           "name": "abc.123",
          |           "limit": 10000,
          |           "aggregators": [
          |               {
          |                   "name": "sum",
          |                   "sampling": {
          |                       "value": "10",
          |                       "unit": "minutes"
          |                   }
          |               }
          |           ]
          |       },
          |       {
          |           "tags": {
          |               "host": ["foo", "foo2"],
          |               "customer": ["bar"]
          |           },
          |           "name": "xyz.123",
          |           "aggregators": [
          |               {
          |                   "name": "avg",
          |                   "sampling": {
          |                       "value": "10",
          |                       "unit": "minutes"
          |                   }
          |               }
          |           ]
          |       }
          |   ]
          |}
        """.stripMargin)
    }

    "Correctly serialize a minimal QueryPlugin" in {
      val queryPlugin = QueryPlugin("testPlugin")
      Json.toJson(queryPlugin) must be equalTo Json.obj("name" -> "testPlugin")
    }

    "Correctly serialize a QueryPlugin with properties" in {
      val queryPlugin = QueryPlugin("testPlugin",
        Map(
          "stringProp" -> "stringVal",
          "intProp" -> 123,
          "doubleProp" -> 1.23d,
          "stringListProp" -> List("one", "two", "three")))
      Json.toJson(queryPlugin) must be equalTo Json.obj(
        "name" -> "testPlugin",
        "stringProp" -> "stringVal",
        "intProp" -> 123,
        "doubleProp" -> 1.23,
        "stringListProp" -> Json.arr("one", "two", "three"))
    }

    "Correctly serialize a query with plugins configured at the Query level" in {
      val query = Query(MetricName("mymetric"), plugins = Seq(QueryPlugin("testPlugin", Map("propA" -> "valA"))))
      Json.toJson(query) must be equalTo Json.obj("name" -> "mymetric", "plugins" -> Json.arr(Json.obj("name" -> "testPlugin", "propA" -> "valA")))
    }

    "Correctly serialize a query with plugins configured at the QueryMetric level" in {
      val qm = QueryMetrics(Seq(Query(MetricName("mymetric"))),
        TimeSpan(AbsoluteStartTime(Instant.ofEpochSecond(1470052425L))),
        plugins = Seq(QueryPlugin("testPlugin", Map("propA" -> "valA"))))

      Json.toJson(qm) should be equalTo Json.obj(
        "start_absolute" -> 1470052425000L,
        "metrics" -> Seq(Json.obj("name" -> "mymetric")),
        "plugins" -> Json.arr(Json.obj("name" -> "testPlugin", "propA" -> "valA"))
      )

    }

  }
}

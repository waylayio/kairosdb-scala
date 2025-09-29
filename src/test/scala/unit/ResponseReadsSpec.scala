package unit

import java.time.Instant

import io.waylay.kairosdb.driver.models.GroupBy.{GroupByTags, GroupByType}
import io.waylay.kairosdb.driver.models.KairosCompatibleType.{KNull, KNumber}
import io.waylay.kairosdb.driver.models.MetricName
import io.waylay.kairosdb.driver.models.QueryResponse.{Response, ResponseQuery, Result, TagResult}
import io.waylay.kairosdb.driver.models.json.Formats.responseReads
import org.specs2.mutable.Specification
import play.api.libs.json._
import scala.collection.immutable.Seq

class ResponseReadsSpec extends Specification {
  "Query response reader" should {
    "Correctly read the example from the KairosDB docs" in {
      val json = Json.parse(
        """
          |{
          |  "queries": [
          |      {
          |          "sample_size": 14368,
          |          "results": [
          |              {
          |                  "name": "abc_123",
          |                  "group_by": [
          |                      {
          |                         "name": "type",
          |                         "type": "number"
          |                      },
          |                      {
          |                         "name": "tag",
          |                         "tags": [
          |                             "host"
          |                         ],
          |                        "group": {
          |                             "host": "server1"
          |                        }
          |                      }
          |                  ],
          |                  "tags": {
          |                      "host": [
          |                          "server1"
          |                      ],
          |                      "customer": [
          |                          "bar"
          |                      ]
          |                  },
          |                  "values": [
          |                      [
          |                          1364968800000,
          |                          11019
          |                      ],
          |                      [
          |                          1366351200000,
          |                          null
          |                      ],
          |                      [
          |                          1366987600000,
          |                          2843
          |                      ]
          |                  ]
          |              }
          |         ]
          |     }
          |  ]
          |}
        """.stripMargin)

      val expected = Response(Seq(
        ResponseQuery(14368,
          Seq(
            Result(
              MetricName("abc_123"),
              Seq(GroupByType("number"),
                GroupByTags(Seq("host"))), // TODO group by tag in the result has an extra field "group"
              Seq(TagResult("host", Seq("server1")), TagResult("customer", Seq("bar"))),
              Seq(
                (Instant.ofEpochMilli(1364968800000L), KNumber(11019), None),
                (Instant.ofEpochMilli(1366351200000L), KNull, None),
                (Instant.ofEpochMilli(1366987600000L), KNumber(2843), None)
              )
            )
          )
        )
      ))

      responseReads.reads(json) must beEqualTo(JsSuccess(expected))
    }

    "Correctly read example without group_by" in {
      val json = Json.parse(
        """
          |{
          |  "queries": [
          |      {
          |          "sample_size": 14368,
          |          "results": [
          |              {
          |                  "name": "abc_123",
          |                  "tags": {
          |                      "host": [
          |                          "server1"
          |                      ],
          |                      "customer": [
          |                          "bar"
          |                      ]
          |                  },
          |                  "values": [
          |                      [
          |                          1364968800000,
          |                          11019
          |                      ],
          |                      [
          |                          1366351200000,
          |                          2843
          |                      ]
          |                  ]
          |              }
          |         ]
          |     }
          |  ]
          |}
        """.stripMargin)

      val expected = Response(Seq(
        ResponseQuery(14368,
          Seq(
            Result(
              MetricName("abc_123"),
              Seq.empty,
              Seq(TagResult("host", Seq("server1")), TagResult("customer", Seq("bar"))),
              Seq(
                (Instant.ofEpochMilli(1364968800000L), KNumber(11019), None),
                (Instant.ofEpochMilli(1366351200000L), KNumber(2843), None)
              )
            )
          )
        )
      ))

      responseReads.reads(json) must beEqualTo(JsSuccess(expected))
    }

    "Correctly read example with ingestion timestamps" in {
      val json = Json.parse(
        """
          |{
          |  "queries": [
          |      {
          |          "sample_size": 2,
          |          "results": [
          |              {
          |                  "name": "metric.with.ingestion.timestamp",
          |                  "tags": {
          |                      "host": [
          |                          "server01"
          |                      ]
          |                  },
          |                  "values": [
          |                      [
          |                          1609459260000,
          |                          42.5,
          |                          1609459261234
          |                      ],
          |                      [
          |                          1609459320000,
          |                          43.1,
          |                          1609459321456
          |                      ]
          |                  ]
          |              }
          |         ]
          |     }
          |  ]
          |}
        """.stripMargin)

      val expected = Response(Seq(
        ResponseQuery(2,
          Seq(
            Result(
              MetricName("metric.with.ingestion.timestamp"),
              Seq.empty,
              Seq(TagResult("host", Seq("server01"))),
              Seq(
                (Instant.ofEpochMilli(1609459260000L), KNumber(42.5), Some(Instant.ofEpochMilli(1609459261234L))),
                (Instant.ofEpochMilli(1609459320000L), KNumber(43.1), Some(Instant.ofEpochMilli(1609459321456L)))
              )
            )
          )
        )
      ))

      responseReads.reads(json) must beEqualTo(JsSuccess(expected))
    }
  }
}

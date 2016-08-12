# kairosdb-scala [![Build Status](https://travis-ci.org/waylayio/kairosdb-scala.svg?branch=master)](https://travis-ci.org/waylayio/kairosdb-scala) [![Coverage Status](https://coveralls.io/repos/github/waylayio/kairosdb-scala/badge.svg)](https://coveralls.io/github/waylayio/kairosdb-scala)

A feature-complete Scala library to talk to KairosDB, a time series database.

![](logo.png)

## Examples

See the `examples` project for some examples.

```
for {
  version <- kairosDB.version
  names   <- kairosDB.listMetricNames
  _       <- kairosDB.addDataPoint(DataPoint(MetricName("kairosdbscala.test"), KNumber(9001), tags = Seq(Tag("awesome", "yes"))))
  qr      <- kairosDB.queryMetrics(
               QueryMetrics(Seq(
                 Query(MetricName("kairosscala.test"), tags = Seq(QueryTag("awesome", Seq("yes", "true"))))
               ), TimeSpan(RelativeStartTime(5.minutes)))
             )
} yield {
  println(s"The KairosDB version is $version.")
  println(s"""Some of the metrics are ${names take 3 map (_.name) mkString ", "}.""")
  println(s"The result of querying was ${qr.queries.head.results.head}.")
}
```

## Features

This library supports all current KairosDB functionality.

| Name               | Supported | Unit tests | Integration tests |
| ------------------ | --- | --- | --- |
| List metric names  | X | X | X |
| List tag names     | X | X |  |
| List tag values    | X | X |  |
| Health status      | X | X | X |
| Health check       | X | X | X |
| Version            | X | X | X |
| Delete metric      | X | X | X |
| Add data points    | X | X | X |
| Delete data points | X | X |  |
| Query metric tags  | X | X | X |
| Aggregators        | X | X |  |
| Query metrics      | See below |  |  |
| [Authentication](https://github.com/waylayio/kairosdb-scala/issues/2) | X |  |  |

### Query metrics

| Name                | Supported | Unit tests | Integration tests |
| ------------------- | --- | --- | --- |
| Grouping            | X | X |  |
| Aggregators         | X | X |  |
| Filtering           | X | X |  |
| Start and end times | X | X |  |
| Cache time          | X | X |  |
| Response            | X | X |  |

## Development


### Testing

| Type              | Package         | Function                                                 |
|-------------------|-----------------|----------------------------------------------------------|
| Unit tests        | `unit.*`        | Verifies the generated JSON against expected JSON        |
| Integration tests | `integration.*` | Starts up KairosDB as a Docker instance and queries that |

The integration tests use [`docker-it-scala`](https://github.com/whisklabs/docker-it-scala). You might have to `export DOCKER_HOST=unix:///var/run/docker.sock` to make it work. Since Docker is used, you don't need a local KairosDB to test against.

You can run the unit tests with `test` and the integration tests with `it:test`. See [here](http://www.scala-sbt.org/0.13/docs/Testing.html#Integration+Tests) for more information on integration tests with sbt.

### Releases/versioning

[`sbt-release`](https://github.com/sbt/sbt-release) is used for releases. Use `sbt release`.

### ScalaDoc

Published [here](https://waylayio.github.io/kairosdb-scala/latest/api) on [Github Pages](https://pages.github.com/) with [sbt-site](https://github.com/sbt/sbt-site) and [sbt-ghpages](https://github.com/sbt/sbt-ghpages). Automatically published as part of release process.

Manually publishing the ScalaDocs can be done with the following command:

```
$ sbt ghpagesPushSite
```


# kairosdb-scala [![Build Status](https://travis-ci.org/waylayio/kairosdb-scala.svg?branch=master)](https://travis-ci.org/waylayio/kairosdb-scala)

A feature-complete Scala library to talk to KairosDB, a time series database.

![](logo.png)

## Usage

Add this to your build.sbt

```scala
libraryDependencies += "io.waylay.kairosdb" %% "kairosdb-scala" % "1.0.0"
```

snapshots are available at: `https://oss.sonatype.org/content/repositories/snapshots`


## Examples

See the `examples` project for some examples.

### Usage on the REPL

If you want to test this project out on the REPL, clone it, switch to the examples project (`project examples`), run `sbt console`, and enter `:load repl/repl.scala` to get started quickly.

### Config

Configuration is done by creating a `KairosDBConfig` instance. The defaults are to connect to a local KairosDB on port 8080.

### Implicits

To make the library more pleasant to use, implicits are provided in `io.waylay.kairosdb.driver.Implicits`:

```scala
import io.waylay.kairosdb.driver.Implicits._
```

### Data types

KairosDB supports numbers and strings. These are represented as `KNumber` and `KString`. Implicit conversions are provided.

```scala
scala> val a = KNumber(1111)
a: io.waylay.kairosdb.driver.models.KairosCompatibleType.KNumber = KNumber(1111)

scala> val b = KString("test")
b: io.waylay.kairosdb.driver.models.KairosCompatibleType.KString = KString(test)

scala> val c: KNumber = 1111
c: io.waylay.kairosdb.driver.models.KairosCompatibleType.KNumber = KNumber(1111)

scala> val d: KString = "test"
d: io.waylay.kairosdb.driver.models.KairosCompatibleType.KString = KString(test)
```

### Data points

You can insert data points with a single value, but you can also batch insert data points with the same tags, ttl and metric name.

```scala
scala> val dp = DataPointWithSingleValue("scala.powerlevel", 9001, Instant.now, Seq(Tag("some", "tag")), Some(5.minutes))
dp: io.waylay.kairosdb.driver.models.DataPointWithSingleValue = DataPointWithSingleValue(MetricName(scala.powerlevel),KNumber(9001),2016-08-11T09:25:09.498Z,List(Tag(some,tag)),Some(5 minutes))

scala> val dp2 = DataPoint("scala.powerlevel", 9001, Instant.now, Seq(Tag("some", "tag")), Some(5.minutes))
dp2: io.waylay.kairosdb.driver.models.DataPointWithSingleValue = DataPointWithSingleValue(MetricName(scala.powerlevel),KNumber(9001),2016-08-11T09:25:28.639Z,List(Tag(some,tag)),Some(5 minutes))

scala> val values: Seq[(Instant, KairosCompatibleType)] = Seq(Instant.ofEpochMilli(1000) -> 10, Instant.ofEpochMilli(2000) -> 20)
values: Seq[(java.time.Instant, io.waylay.kairosdb.driver.models.KairosCompatibleType)] = List((1970-01-01T00:00:01Z,KNumber(10)), (1970-01-01T00:00:02Z,KNumber(20)))

scala> val dps = DataPointWithMultipleValues("scala.powerlevel", values, Seq(Tag("shared", "yes"), Tag("cool", "very")))
dps: io.waylay.kairosdb.driver.models.DataPointWithMultipleValues = DataPointWithMultipleValues(MetricName(scala.powerlevel),List((1970-01-01T00:00:01Z,KNumber(10)), (1970-01-01T00:00:02Z,KNumber(20))),List(Tag(shared,yes), Tag(cool,very)),None)
```

### Inserting data

```scala
scala> kairosDB.addDataPoint(dp).map(_ => println("Inserted data point!"))
res2: scala.concurrent.Future[Unit] = List()

Inserted data point!

scala> kairosDB.addDataPoint(dps).map(_ => println("Inserted data point with multiple values!"))
res4: scala.concurrent.Future[Unit] = List()

Inserted data point with multiple values!

scala> kairosDB.addDataPoints(Seq(dp, dps)).map(_ => println("Inserted multiple data points!"))
res6: scala.concurrent.Future[Unit] = List()

Inserted multiple data points!
```

### Querying

You can build queries with the `QueryMetrics` class. It allows to send multiple queries at once.

Firing off the query is done with `KairosDB#queryMetrics`. `KairosDB#queryMetricTags` and `KairosDB#deleteDataPoints` also accept a `QueryMetrics`.

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


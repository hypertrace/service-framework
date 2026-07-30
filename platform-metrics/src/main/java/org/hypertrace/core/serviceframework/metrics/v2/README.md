# Metrics v2

Scoped metrics API for services and Flink jobs. Prefer this over calling Micrometer or
`PlatformMetricsRegistry.register*` directly when you want:

- a small surface (`Counter` / `Timer` / gauges) that is independent of the backend
- static tags bound at meter creation, plus optional runtime tags per observation
- the same wiring for JVM services and Flink subtasks

Package: `org.hypertrace.core.serviceframework.metrics.v2`

## Concepts

| Type | Role |
| --- | --- |
| `MetricRegistry` | Factory for counters, timers, gauges, and async counters |
| `Tag` | One dimension (`key`, `value`) |
| `Counter` | Monotonic counter; supports runtime tags on `increment` |
| `Timer` | Latency distribution; supports runtime tags on `record` |
| `MetricRegistryFactory` | Entry point: JVM-wide or Flink-subtask-scoped registry |
| `MetricCollectingClientInterceptor` | gRPC client interceptor that reports through a `MetricRegistry` |

Tag order matters for identity. Pass the same tags in the same order to address the same
series; differently ordered tags may become distinct series.

## Prometheus limitation: stable tag key set per metric name

Prometheus (and Micrometer's Prometheus registry) requires that every time series that
shares a metric **name** carry the **same set of tag keys**. Values may differ; keys may
not.

That means, for a given name:

- Always observe with the same runtime tag keys, **or**
- Always observe with no runtime tags

Do **not** mix the two, and do not add/remove keys across observations of the same name.
Inconsistent key sets cause later registrations to be rejected and those series to be
dropped from scrapes (often leaving only an always-zero, earlier series).

v2 counters and timers register lazily on first observation for this reason — an eager
untagged base meter would lock the key set without the runtime tags and break every
subsequent tagged recording.

```java
// Good: every increment of "enricher.requests" includes the "tenant" key
Counter requests = metrics.counter("enricher.requests", new Tag("enricher", "api"));
requests.increment(1, new Tag("tenant", "t1"));
requests.increment(1, new Tag("tenant", "t2"));

// Bad: first observation has no "tenant" key; later ones do
requests.increment();                         // locks key set without "tenant"
requests.increment(1, new Tag("tenant", "t1")); // rejected / dropped under Prometheus
```

Static tags passed to `counter` / `timer` / `gauge` are fine — they are present on every
series for that meter. The rule applies to the **combined** key set (scope + static +
runtime) that ends up on the scraped series.

## Obtaining a registry

### JVM / ordinary services

Initialize the platform registry once at process start (existing API), then take a
JVM-wide v2 view:

```java
PlatformMetricsRegistry.initMetricsRegistry(serviceName, metricsConfig);

MetricRegistry metrics = MetricRegistryFactory.forJvm();
```

`forJvm()` is lazy and process-wide: every caller shares the first instance.

### Flink subtask

In an operator / function with a `RuntimeContext`:

```java
MetricRegistry metrics = MetricRegistryFactory.forSubtask(getRuntimeContext());
```

That view reports through the process Micrometer registry and attaches
`task.name` / `subtask.index` as scope tags.

For Flink, bind the scrape endpoint once via `SharedMeterRegistry` (typically from the
Prometheus metric reporter at TaskManager startup) so system and user-code classloaders
share one registry:

```java
SharedMeterRegistry.getOrCreate(serviceName, exporterPort);
```

`getOrCreate` takes only `String` / `int` on purpose so the parent-first shared surface
does not depend on typesafe-config. Put typesafe-config on the Flink lib classpath if
`PlatformMetricsRegistry` still references it; it does **not** need a parent-first
pattern when `Config` never crosses the shared API.

## Recording metrics

```java
MetricRegistry metrics = MetricRegistryFactory.forJvm();

// Static tags bound for every increment of this counter
Counter requests = metrics.counter("enricher.requests", new Tag("enricher", "api"));
requests.increment();
requests.increment(5);

// Runtime tags — use the same keys on every observation of this name (see Prometheus
// limitation above). Resolved/cached per tag set on the hot path.
Counter byTenant = metrics.counter("enricher.requests.by_tenant", new Tag("enricher", "api"));
byTenant.increment(1, new Tag("tenant", tenantId));

Timer latency = metrics.timer("enricher.latency", new Tag("enricher", "api"));
latency.record(Duration.ofMillis(12), new Tag("outcome", "success"));
latency.record(Duration.ofMillis(8), new Tag("outcome", "failure"));

// Gauge: keep a strong reference to `cache` for as long as the gauge should report
metrics.gauge("cache.size", cache, Cache::size, new Tag("cache", "entities"));

// Async counter: sample a monotonic total already tracked elsewhere
metrics.asyncCounter("cache.hits", cache, Cache::hitCount, new Tag("cache", "entities"));
```

## gRPC client metrics

```java
MetricRegistry metrics = MetricRegistryFactory.forJvm();
Channel channel =
    ClientInterceptors.intercept(
        channel, new MetricCollectingClientInterceptor(metrics));
```

Per full method name the interceptor records:

| Metric | Tags |
| --- | --- |
| `grpc.client.requests.sent` | `service`, `method`, `methodType` |
| `grpc.client.responses.received` | `service`, `method`, `methodType` |
| `grpc.client.processing.duration` | above + `statusCode` |

Meters for a method are registered once and reused; that avoids duplicate registration
errors on strict backends (e.g. Flink `MetricGroup`).

`io.grpc` is `compileOnly` for this module — the consuming service must provide gRPC
on its runtime classpath.

## Relation to `PlatformMetricsRegistry`

| | `PlatformMetricsRegistry` | v2 (`MetricRegistry`) |
| --- | --- | --- |
| Surface | Micrometer types directly | `Counter` / `Timer` / `Tag` abstraction |
| Init / JVM / scrape | Owns reporters, common tags, `/metrics` | Consumes the shared Micrometer registry |
| Flink | N/A | `forSubtask` + `SharedMeterRegistry` |
| Prefer when | Existing callers, admin scrape setup | New application metrics, gRPC, Flink |

v2 does not replace process bootstrap. Still call
`PlatformMetricsRegistry.initMetricsRegistry(...)` (or `SharedMeterRegistry.getOrCreate`
in Flink) before `forJvm()` / `forSubtask()`.

## Dependencies for consumers

```kotlin
implementation(projects.platformMetrics)

// Only if you use MetricCollectingClientInterceptor:
implementation(commonLibs.grpc.api) // or your gRPC stack

// Only if you call MetricRegistryFactory.forSubtask:
// Flink APIs must be on the compile/runtime classpath of the Flink job
```

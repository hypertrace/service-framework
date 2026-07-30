package org.hypertrace.core.serviceframework.metrics.v2.micrometer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.hypertrace.core.serviceframework.metrics.v2.Counter;
import org.hypertrace.core.serviceframework.metrics.v2.MetricRegistry;
import org.hypertrace.core.serviceframework.metrics.v2.Tag;
import org.hypertrace.core.serviceframework.metrics.v2.Timer;
import org.junit.jupiter.api.Test;

class MicrometerMetricRegistryTest {

  @Test
  void testCounterAccumulates() {
    SimpleMeterRegistry backend = new SimpleMeterRegistry();
    MetricRegistry registry = MicrometerMetricRegistry.create(backend);
    Counter counter = registry.counter("requests");
    counter.increment();
    counter.increment(3);
    assertEquals(4.0, backend.get("requests").counter().count());
  }

  @Test
  void testScopeAndStaticTagsApplied() {
    SimpleMeterRegistry backend = new SimpleMeterRegistry();
    MetricRegistry registry = MicrometerMetricRegistry.create(backend, new Tag("region", "us"));
    registry.counter("requests", new Tag("stage", "parse")).increment(2);
    assertEquals(
        2.0, backend.get("requests").tags("region", "us", "stage", "parse").counter().count());
  }

  @Test
  void testRuntimeTagsCreateSeparateCounters() {
    SimpleMeterRegistry backend = new SimpleMeterRegistry();
    MetricRegistry registry = MicrometerMetricRegistry.create(backend);
    Counter counter = registry.counter("requests");
    counter.increment(1, new Tag("tenant", "t1"));
    counter.increment(2, new Tag("tenant", "t2"));
    counter.increment(5, new Tag("tenant", "t1"));
    assertEquals(6.0, backend.get("requests").tags("tenant", "t1").counter().count());
    assertEquals(2.0, backend.get("requests").tags("tenant", "t2").counter().count());
  }

  @Test
  void testRuntimeTagOrderDoesNotAffectIdentity() {
    SimpleMeterRegistry backend = new SimpleMeterRegistry();
    MetricRegistry registry = MicrometerMetricRegistry.create(backend);
    Counter counter = registry.counter("requests");
    counter.increment(1, new Tag("a", "1"), new Tag("b", "2"));
    counter.increment(1, new Tag("b", "2"), new Tag("a", "1"));
    assertEquals(2.0, backend.get("requests").tags("a", "1", "b", "2").counter().count());
  }

  @Test
  void testGaugeSamplesCurrentValue() {
    SimpleMeterRegistry backend = new SimpleMeterRegistry();
    MetricRegistry registry = MicrometerMetricRegistry.create(backend);
    AtomicLong value = new AtomicLong(7);
    registry.gauge("cache.size", value, AtomicLong::get, new Tag("cache", "c1"));
    assertEquals(7.0, backend.get("cache.size").tags("cache", "c1").gauge().value());
    value.set(11);
    assertEquals(11.0, backend.get("cache.size").tags("cache", "c1").gauge().value());
  }

  @Test
  void testAsyncCounterSamplesCurrentValue() {
    SimpleMeterRegistry backend = new SimpleMeterRegistry();
    MetricRegistry registry = MicrometerMetricRegistry.create(backend);
    AtomicLong hits = new AtomicLong(3);
    registry.asyncCounter("cache.gets", hits, AtomicLong::get, new Tag("result", "hit"));
    assertEquals(3.0, backend.get("cache.gets").tags("result", "hit").functionCounter().count());
    hits.set(8);
    assertEquals(8.0, backend.get("cache.gets").tags("result", "hit").functionCounter().count());
  }

  @Test
  void testAsyncCounterScopeAndStaticTagsApplied() {
    SimpleMeterRegistry backend = new SimpleMeterRegistry();
    MetricRegistry registry = MicrometerMetricRegistry.create(backend, new Tag("region", "us"));
    AtomicLong evictions = new AtomicLong(5);
    registry.asyncCounter("cache.evictions", evictions, AtomicLong::get, new Tag("cache", "c1"));
    assertEquals(
        5.0,
        backend
            .get("cache.evictions")
            .tags("region", "us", "cache", "c1")
            .functionCounter()
            .count());
  }

  @Test
  void testTimerRecordsCountAndTotalTime() {
    SimpleMeterRegistry backend = new SimpleMeterRegistry();
    MetricRegistry registry = MicrometerMetricRegistry.create(backend);
    Timer timer = registry.timer("latency");
    timer.record(Duration.ofMillis(10));
    timer.record(Duration.ofMillis(30));
    assertEquals(2L, backend.get("latency").timer().count());
    assertEquals(40.0, backend.get("latency").timer().totalTime(TimeUnit.MILLISECONDS));
  }

  @Test
  void testTimerNoRuntimeTagsRecordsOnce() {
    SimpleMeterRegistry backend = new SimpleMeterRegistry();
    MetricRegistry registry = MicrometerMetricRegistry.create(backend);
    Timer timer = registry.timer("latency");
    timer.record(Duration.ofMillis(10), new Tag[0]);
    assertEquals(1L, backend.get("latency").timer().count());
  }

  @Test
  void testTimerScopeAndStaticTagsApplied() {
    SimpleMeterRegistry backend = new SimpleMeterRegistry();
    MetricRegistry registry = MicrometerMetricRegistry.create(backend, new Tag("region", "us"));
    registry.timer("latency", new Tag("stage", "parse")).record(Duration.ofMillis(5));
    assertEquals(1L, backend.get("latency").tags("region", "us", "stage", "parse").timer().count());
  }

  @Test
  void testTimerRuntimeTagsCreateSeparateTimers() {
    SimpleMeterRegistry backend = new SimpleMeterRegistry();
    MetricRegistry registry = MicrometerMetricRegistry.create(backend);
    Timer timer = registry.timer("latency");
    timer.record(Duration.ofMillis(10), new Tag("tenant", "t1"));
    timer.record(Duration.ofMillis(20), new Tag("tenant", "t2"));
    timer.record(Duration.ofMillis(30), new Tag("tenant", "t1"));
    assertEquals(2L, backend.get("latency").tags("tenant", "t1").timer().count());
    assertEquals(1L, backend.get("latency").tags("tenant", "t2").timer().count());
  }

  @Test
  void testTimerRuntimeTagOrderDoesNotAffectIdentity() {
    SimpleMeterRegistry backend = new SimpleMeterRegistry();
    MetricRegistry registry = MicrometerMetricRegistry.create(backend);
    Timer timer = registry.timer("latency");
    timer.record(Duration.ofMillis(10), new Tag("a", "1"), new Tag("b", "2"));
    timer.record(Duration.ofMillis(10), new Tag("b", "2"), new Tag("a", "1"));
    assertEquals(2L, backend.get("latency").tags("a", "1", "b", "2").timer().count());
  }

  // Regression: the Prometheus registry rejects a metric name registered with inconsistent tag
  // keys. An eagerly-registered untagged base timer would fix the key set without the runtime tag
  // and cause every subsequent tagged recording to be dropped, leaving only an always-zero,
  // untagged series. The timer must not register anything until the first (tagged) recording.
  @Test
  void testTimerRuntimeTagsScrapeableUnderPrometheusRegistry() {
    PrometheusMeterRegistry backend = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    MetricRegistry registry =
        MicrometerMetricRegistry.create(backend, new Tag("sub.task.index", "0"));
    Timer timer = registry.timer("structured.trace.enrichment.timer");
    timer.record(Duration.ofMillis(10), new Tag("enricher.type", "apiAttributeEnricher"));
    timer.record(Duration.ofMillis(20), new Tag("enricher.type", "httpAttributeEnricher"));

    assertEquals(
        1L,
        backend
            .get("structured.trace.enrichment.timer")
            .tags("sub.task.index", "0", "enricher.type", "apiAttributeEnricher")
            .timer()
            .count());
    assertEquals(
        1L,
        backend
            .get("structured.trace.enrichment.timer")
            .tags("sub.task.index", "0", "enricher.type", "httpAttributeEnricher")
            .timer()
            .count());
  }

  @Test
  void testCounterRuntimeTagsScrapeableUnderPrometheusRegistry() {
    PrometheusMeterRegistry backend = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    MetricRegistry registry =
        MicrometerMetricRegistry.create(backend, new Tag("sub.task.index", "0"));
    Counter counter = registry.counter("structured.trace.enrichment.failure");
    counter.increment(1, new Tag("failure.type", "exception"));
    counter.increment(1, new Tag("failure.type", "timeout"));

    assertEquals(
        1.0,
        backend
            .get("structured.trace.enrichment.failure")
            .tags("sub.task.index", "0", "failure.type", "exception")
            .counter()
            .count());
    assertEquals(
        1.0,
        backend
            .get("structured.trace.enrichment.failure")
            .tags("sub.task.index", "0", "failure.type", "timeout")
            .counter()
            .count());
  }
}

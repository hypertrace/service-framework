package org.hypertrace.core.serviceframework.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PlatformMetricsRegistry} */
public class PlatformMetricsRegistryTest {
  private static final String PUSH_GATEWAY_REPORTER_NAME = "pushgateway";
  private static final String PROMETHEUS_REPORTER_NAME = "prometheus";

  private static void initializeCustomRegistry(List<String> reporters) {
    PlatformMetricsRegistry.initMetricsRegistry(
        "test-service",
        ConfigFactory.parseMap(
            Map.of(
                "reporter.names",
                reporters,
                "reporter.prefix",
                "test-service",
                "reportInterval",
                "10",
                "defaultTags",
                List.of("test.name", "PlatformMetricsRegistryTest"))));
  }

  @AfterEach
  public void stopRegistry() {
    PlatformMetricsRegistry.stop();
  }

  @Test
  public void testMetricRegistryInitialization() {
    // Make sure logging reporter initialization doesn't fail.
    initializeCustomRegistry(List.of("logging"));

    Timer timer = PlatformMetricsRegistry.registerTimer("my.timer", Map.of("foo", "bar"));
    timer.record(1, TimeUnit.SECONDS);

    // Try calling init again but that should be a NoOp.
    initializeCustomRegistry(List.of("logging"));
  }

  @Test
  public void testMetricRegistryStop() {
    // Make sure logging reporter initialization doesn't fail.
    initializeCustomRegistry(List.of("logging", "prometheus"));

    Timer timer = PlatformMetricsRegistry.registerTimer("my.timer", Map.of("foo", "bar"));
    timer.record(1, TimeUnit.SECONDS);

    PlatformMetricsRegistry.stop();
    assertEquals(
        0,
        ((CompositeMeterRegistry) PlatformMetricsRegistry.getMeterRegistry())
            .getRegistries()
            .size());
  }

  @Test
  public void testTimer() {
    initializeCustomRegistry(List.of("testing"));

    Timer timer = PlatformMetricsRegistry.registerTimer("my.timer", Map.of("foo", "bar"));
    timer.record(1, TimeUnit.SECONDS);
    assertEquals(1, timer.count());
    assertEquals(1, timer.totalTime(TimeUnit.SECONDS));

    // Try to register the same timer again and we should get the same instance.
    timer = PlatformMetricsRegistry.registerTimer("my.timer", Map.of("foo", "bar"));
    timer.record(2, TimeUnit.SECONDS);
    assertEquals(2, timer.count());
    assertEquals(3, timer.totalTime(TimeUnit.SECONDS));
    assertEquals(2, timer.max(TimeUnit.SECONDS));

    // Change the tag and try.
    timer = PlatformMetricsRegistry.registerTimer("my.timer", Map.of("foo", "bar1"));
    timer.record(1, TimeUnit.SECONDS);
    assertEquals(1, timer.count());
  }

  @Test
  public void testCounter() {
    PlatformMetricsRegistry.initMetricsRegistry("test-service", ConfigFactory.empty());

    Counter counter = PlatformMetricsRegistry.registerCounter("my.counter", Map.of("foo", "bar"));
    counter.increment();
    assertEquals(1, counter.count());

    counter = PlatformMetricsRegistry.registerCounter("my.counter", Map.of("foo", "bar"));
    counter.increment(9);
    assertEquals(10, counter.count());

    counter = PlatformMetricsRegistry.registerCounter("my.counter", Map.of("foo", "bar1"));
    counter.increment();
    assertEquals(1, counter.count());
  }

  @Test
  public void testGauge() {
    initializeCustomRegistry(List.of("testing"));

    AtomicInteger atomicInteger = new AtomicInteger(1);
    AtomicInteger gauge =
        PlatformMetricsRegistry.registerGauge("my.gauge", Map.of("foo", "bar"), atomicInteger);
    atomicInteger.incrementAndGet();
    assertEquals(2, gauge.get());

    // Register a new instance as a Gauge and the value changes though the tags haven't changed.
    AtomicInteger newAtomicInteger = new AtomicInteger(1);
    gauge =
        PlatformMetricsRegistry.registerGauge("my.gauge", Map.of("foo", "bar"), newAtomicInteger);
    newAtomicInteger.addAndGet(10);
    assertEquals(11, gauge.get());
  }

  @Test
  public void testDistributionSummary() {
    initializeCustomRegistry(List.of("testing"));

    DistributionSummary distribution =
        PlatformMetricsRegistry.registerDistributionSummary(
            "my.distribution", Map.of("foo", "bar"));
    distribution.record(100);
    assertEquals(1, distribution.count());
    assertEquals(100, distribution.totalAmount());

    // Try to register the same summary again and we should get the same instance.
    distribution =
        PlatformMetricsRegistry.registerDistributionSummary(
            "my.distribution", Map.of("foo", "bar"));
    distribution.record(50);
    assertEquals(2, distribution.count());
    assertEquals(150, distribution.totalAmount());
    assertEquals(75, distribution.mean());
    assertTrue(
        Arrays.stream(distribution.takeSnapshot().percentileValues())
            .map(m -> m.percentile())
            .collect(Collectors.toList())
            .containsAll(List.of(0.5, 0.95, 0.99)));

    // Create a new distribution with histogram enabled
    distribution =
        PlatformMetricsRegistry.registerDistributionSummary(
            "my.distribution", new HashMap<>(), true);
    distribution.record(100);
    assertEquals(1, distribution.count());
    assertEquals(100, distribution.totalAmount());
    assertTrue(
        Arrays.stream(distribution.takeSnapshot().percentileValues())
            .map(m -> m.percentile())
            .collect(Collectors.toList())
            .containsAll(List.of(0.5, 0.95, 0.99)));
  }

  @Test
  public void testCache() throws Exception {
    initializeCustomRegistry(List.of("testing"));
    Cache<String, Integer> cache = CacheBuilder.newBuilder().maximumSize(10).recordStats().build();
    PlatformMetricsRegistry.registerCache("my.cache", cache, Map.of("foo", "bar"));
    Callable<Integer> loader =
        new Callable<Integer>() {
          @Override
          public Integer call() throws Exception {
            return -1;
          }
        };

    // Doing some cache activity
    cache.put("One", 1);
    cache.put("Two", 2);
    cache.get("One", loader); // hit
    cache.get("Two", loader); // hit
    cache.get("Two", loader); // hit
    cache.get("Three", loader); // miss hence loaded from loader
    cache.get("Failed", loader); // miss hence loaded from loader
    /*
    * Cache = {One,Two, Three, Failed}
    * Cache Hit is basically the number of times cache.get returned an entry which was present in the cache, hence hit = 3
    * Cache Miss is basically the number of times cache.get returned an entry not present in the cache (hence loaded from loader), hence miss = 2
    * The way cache.get works is that if there is a cache miss then the entry is loaded from the loader and included in the cache,
    hence the cache size due to the above activity would be 2 (already put One,Two) + 2 (cache miss on Three, Failed) = 4

    Expected hit count = 3.0, miss count = 2.0, cache size = 4
     */

    // Checking Cache Stats from registry
    double hits = 0, misses = 0, size = 0;
    hits =
        PlatformMetricsRegistry.getMeterRegistry()
            .get("cache.gets")
            .tag("result", "hit")
            .meter()
            .measure()
            .iterator()
            .next()
            .getValue();
    misses =
        PlatformMetricsRegistry.getMeterRegistry()
            .get("cache.gets")
            .tag("result", "miss")
            .meter()
            .measure()
            .iterator()
            .next()
            .getValue();
    size =
        PlatformMetricsRegistry.getMeterRegistry()
            .get("cache.size")
            .meter()
            .measure()
            .iterator()
            .next()
            .getValue();

    assertEquals(3.0, hits);
    assertEquals(2.0, misses);
    assertEquals(4.0, size);

    // Doing some more cache activity
    cache.get("NotPresent", loader); // miss hence loaded from loader

    // Cache = {One,Two,Three,Failed,NotPresent}
    // expected hit=3.0, miss=3.0, size=5.0
    hits =
        PlatformMetricsRegistry.getMeterRegistry()
            .get("cache.gets")
            .tag("result", "hit")
            .meter()
            .measure()
            .iterator()
            .next()
            .getValue();
    misses =
        PlatformMetricsRegistry.getMeterRegistry()
            .get("cache.gets")
            .tag("result", "miss")
            .meter()
            .measure()
            .iterator()
            .next()
            .getValue();
    size =
        PlatformMetricsRegistry.getMeterRegistry()
            .get("cache.size")
            .meter()
            .measure()
            .iterator()
            .next()
            .getValue();

    assertEquals(3.0, hits);
    assertEquals(3.0, misses);
    assertEquals(5.0, size);
  }

  @Test
  public void testCaffeine() throws Exception {
    initializeCustomRegistry(List.of("testing"));
    com.github.benmanes.caffeine.cache.Cache<String, Integer> cache =
        Caffeine.newBuilder().maximumSize(10).recordStats().build();
    PlatformMetricsRegistry.registerCaffeine("my.cache", cache, Map.of("foo", "bar"));
    Callable<Integer> loader =
        new Callable<Integer>() {
          @Override
          public Integer call() throws Exception {
            return -1;
          }
        };

    // Doing some cache activity
    cache.put("One", 1);
    cache.put("Two", 2);
    cache.get("One", x -> -1); // hit
    cache.get("Two", x -> -1); // hit
    cache.get("Two", x -> -1); // hit
    cache.get("Three", x -> -1); // miss hence loaded from loader
    cache.get("Failed", x -> -1); // miss hence loaded from loader
    /*
    * Cache = {One,Two, Three, Failed}
    * Cache Hit is basically the number of times cache.get returned an entry which was present in the cache, hence hit = 3
    * Cache Miss is basically the number of times cache.get returned an entry not present in the cache (hence loaded from loader), hence miss = 2
    * The way cache.get works is that if there is a cache miss then the entry is loaded from the loader and included in the cache,
    hence the cache size due to the above activity would be 2 (already put One,Two) + 2 (cache miss on Three, Failed) = 4

    Expected hit count = 3.0, miss count = 2.0, cache size = 4
     */

    // Checking Cache Stats from registry
    double hits = 0, misses = 0, size = 0;
    hits =
        PlatformMetricsRegistry.getMeterRegistry()
            .get("cache.gets")
            .tag("result", "hit")
            .meter()
            .measure()
            .iterator()
            .next()
            .getValue();
    misses =
        PlatformMetricsRegistry.getMeterRegistry()
            .get("cache.gets")
            .tag("result", "miss")
            .meter()
            .measure()
            .iterator()
            .next()
            .getValue();
    size =
        PlatformMetricsRegistry.getMeterRegistry()
            .get("cache.size")
            .meter()
            .measure()
            .iterator()
            .next()
            .getValue();

    assertEquals(3.0, hits);
    assertEquals(2.0, misses);
    assertEquals(4.0, size);

    // Doing some more cache activity
    cache.get("NotPresent", x -> -1); // miss hence loaded from loader

    // Cache = {One,Two,Three,Failed,NotPresent}
    // expected hit=3.0, miss=3.0, size=5.0
    hits =
        PlatformMetricsRegistry.getMeterRegistry()
            .get("cache.gets")
            .tag("result", "hit")
            .meter()
            .measure()
            .iterator()
            .next()
            .getValue();
    misses =
        PlatformMetricsRegistry.getMeterRegistry()
            .get("cache.gets")
            .tag("result", "miss")
            .meter()
            .measure()
            .iterator()
            .next()
            .getValue();
    size =
        PlatformMetricsRegistry.getMeterRegistry()
            .get("cache.size")
            .meter()
            .measure()
            .iterator()
            .next()
            .getValue();

    assertEquals(3.0, hits);
    assertEquals(3.0, misses);
    assertEquals(5.0, size);
  }

  @Test
  public void test_initializePrometheusPushGateway_withNullUrlAddress_throwsException() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> initializeCustomRegistry(List.of(PUSH_GATEWAY_REPORTER_NAME)));
  }

  @Test
  public void test_init_withBothPromethuesAndPushGateway_throwsException() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            initializeCustomRegistry(
                List.of(PUSH_GATEWAY_REPORTER_NAME, PROMETHEUS_REPORTER_NAME)));
  }

  @Test
  public void test_pushMetrics() throws InterruptedException {
    Config config =
        ConfigFactory.parseMap(
            Map.of(
                "reporter.names", List.of(PUSH_GATEWAY_REPORTER_NAME),
                "reporter.prefix", "ines-service",
                "reportInterval", "10",
                "defaultTags", List.of("test.name", "PlatformMetricsRegistryTest"),
                "pushUrlAddress", "localhost:9091"));

    PlatformMetricsRegistry.initMetricsRegistry("ines-service", config);
    Counter counter = PlatformMetricsRegistry.registerCounter("my.counter", Map.of("foo", "bar"));
    counter.increment();
    Assertions.assertEquals(1, counter.count());
  }
}

package org.hypertrace.core.serviceframework.metrics;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    cache.put("One", 1);
    Cache<String, Integer> monitor = PlatformMetricsRegistry.registerCache("my.cache", cache);
    Callable<Integer> loader =
        new Callable<Integer>() {
          @Override
          public Integer call() throws Exception {
            return -1;
          }
        };

    // Try catch block for cache.get [Note this doesn't catch the error thrown if assertion fails
    assertEquals(monitor.get("One", loader), 1);
    cache.put("Two", 2);
    assertEquals(monitor.get("Two", loader), 2);
    assertEquals(cache.get("IsNotPresent", loader), -1);

    // Checking cache stats
    assertEquals(monitor.stats().hitCount(), 2);
    assertEquals(monitor.stats().missCount(), 1);

    // Registering new cache, values should change
    Cache<String, Integer> cache1 = CacheBuilder.newBuilder().maximumSize(10).build();
    monitor = PlatformMetricsRegistry.registerCache("my.cache", cache1);
    assertEquals(monitor.get("First", loader), -1);
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

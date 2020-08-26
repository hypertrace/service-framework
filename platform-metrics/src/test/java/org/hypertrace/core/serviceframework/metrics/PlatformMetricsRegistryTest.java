package org.hypertrace.core.serviceframework.metrics;

import com.typesafe.config.ConfigFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link PlatformMetricsRegistry}
 */
public class PlatformMetricsRegistryTest {
  private static void initializeCustomRegistry(List<String> reporters) {
    PlatformMetricsRegistry.initMetricsRegistry("test-service",
        ConfigFactory.parseMap(Map.of(
            "reporter.names", reporters,
            "reporter.prefix", "test-service",
            "reportInterval", "10",
            "defaultTags", List.of("test.name", "PlatformMetricsRegistryTest")
        )));
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
    assertEquals(0, ((CompositeMeterRegistry)PlatformMetricsRegistry.getMeterRegistry()).getRegistries().size());
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
    AtomicInteger gauge = PlatformMetricsRegistry.registerGauge("my.gauge",
        Map.of("foo", "bar"), atomicInteger);
    atomicInteger.incrementAndGet();
    assertEquals(2, gauge.get());

    // Register a new instance as a Gauge and the value changes though the tags haven't changed.
    AtomicInteger newAtomicInteger = new AtomicInteger(1);
    gauge = PlatformMetricsRegistry.registerGauge("my.gauge",
        Map.of("foo", "bar"), newAtomicInteger);
    newAtomicInteger.addAndGet(10);
    assertEquals(11, gauge.get());
  }
}

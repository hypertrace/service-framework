package org.hypertrace.core.serviceframework.metrics;

import com.typesafe.config.ConfigFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PlatformMetricsRegistry}
 */
public class PlatformMetricsRegistryTest {
  @BeforeAll
  static void initialize() {
    PlatformMetricsRegistry.initMetricsRegistry("test-service",
        ConfigFactory.parseMap(Map.of(
            "reporter.names", "testing",
            "reporter.prefix", "test-service",
            "reportInterval", "10",
            "defaultTags", "test.name,PlatformMetricsRegistryTest"
        )));
  }

  @Test
  public void testTimer() {
    Timer timer = PlatformMetricsRegistry.registerTimer("my.timer", Map.of("foo", "bar"));
    timer.record(1, TimeUnit.SECONDS);
    Assertions.assertEquals(1, timer.count());
    Assertions.assertEquals(1, timer.totalTime(TimeUnit.SECONDS));

    // Try to register the same timer again and we should get the same instance.
    timer = PlatformMetricsRegistry.registerTimer("my.timer", Map.of("foo", "bar"));
    timer.record(2, TimeUnit.SECONDS);
    Assertions.assertEquals(2, timer.count());
    Assertions.assertEquals(3, timer.totalTime(TimeUnit.SECONDS));
    Assertions.assertEquals(2, timer.max(TimeUnit.SECONDS));

    // Change the tag and try.
    timer = PlatformMetricsRegistry.registerTimer("my.timer", Map.of("foo", "bar1"));
    timer.record(1, TimeUnit.SECONDS);
    Assertions.assertEquals(1, timer.count());
  }

  @Test
  public void testCounter() {
    Counter counter = PlatformMetricsRegistry.registerCounter("my.counter", Map.of("foo", "bar"));
    counter.increment();
    Assertions.assertEquals(1, counter.count());

    counter = PlatformMetricsRegistry.registerCounter("my.counter", Map.of("foo", "bar"));
    counter.increment(9);
    Assertions.assertEquals(10, counter.count());

    counter = PlatformMetricsRegistry.registerCounter("my.counter", Map.of("foo", "bar1"));
    counter.increment();
    Assertions.assertEquals(1, counter.count());
  }

  @Test
  public void testGauge() {
    AtomicInteger atomicInteger = new AtomicInteger(1);
    AtomicInteger gauge = PlatformMetricsRegistry.registerGauge("my.gauge",
        Map.of("foo", "bar"), atomicInteger);
    atomicInteger.incrementAndGet();
    Assertions.assertEquals(2, gauge.get());

    // Register a new instance as a Gauge and the value changes though the tags haven't changed.
    AtomicInteger newAtomicInteger = new AtomicInteger(1);
    gauge = PlatformMetricsRegistry.registerGauge("my.gauge",
        Map.of("foo", "bar"), newAtomicInteger);
    newAtomicInteger.addAndGet(10);
    Assertions.assertEquals(11, gauge.get());
  }
}

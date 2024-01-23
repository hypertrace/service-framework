package org.hypertrace.core.serviceframework.docstore.metrics.model;

import static org.hypertrace.core.serviceframework.metrics.PlatformMetricsRegistry.toIterable;

import io.micrometer.core.instrument.Gauge;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Builder;

@Builder
public class GaugeBuilderProvider<T extends Number> {
  private final String name;
  private final Map<String, String> tags;
  private final T number;
  private final Duration duration;
  private final ScheduledExecutorService executor;

  public Gauge.Builder<T> get() {
    // Not creating a strong reference which indefinitely avoids the number from being GCed
    holdNumberInMemoryForDuration();
    return Gauge.builder(name, number, Number::doubleValue).tags(toIterable(tags));
  }

  private void holdNumberInMemoryForDuration() {
    executor.schedule(() -> number, duration.toSeconds(), TimeUnit.SECONDS);
  }
}

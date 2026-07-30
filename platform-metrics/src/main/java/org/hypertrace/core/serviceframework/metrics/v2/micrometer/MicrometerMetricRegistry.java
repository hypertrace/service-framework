package org.hypertrace.core.serviceframework.metrics.v2.micrometer;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;
import org.hypertrace.core.serviceframework.metrics.v2.Counter;
import org.hypertrace.core.serviceframework.metrics.v2.MetricRegistry;
import org.hypertrace.core.serviceframework.metrics.v2.Tag;
import org.hypertrace.core.serviceframework.metrics.v2.Timer;

/**
 * Micrometer-backed {@link MetricRegistry}. Scope tags shared by every metric created through this
 * registry are applied to each counter; per-metric static tags and per-increment runtime tags are
 * layered on top by the individual counters.
 */
public final class MicrometerMetricRegistry implements MetricRegistry {
  private final MeterRegistry delegate;
  private final Tags scopeTags;

  MicrometerMetricRegistry(MeterRegistry delegate, Tag... scopeTags) {
    this.delegate = delegate;
    this.scopeTags = MicrometerTags.of(scopeTags);
  }

  /**
   * Creates a Micrometer-backed registry that reports through the supplied meter registry.
   *
   * @param delegate the Micrometer meter registry metrics are registered with
   * @param scopeTags tags applied to every metric created through this registry
   * @return a registry that reports through Micrometer
   */
  public static MicrometerMetricRegistry create(MeterRegistry delegate, Tag... scopeTags) {
    return new MicrometerMetricRegistry(delegate, scopeTags);
  }

  @Override
  public Counter counter(String name, Tag... tags) {
    return new MicrometerCounter(delegate, name, scopeTags.and(MicrometerTags.of(tags)));
  }

  @Override
  public <T> void gauge(String name, T state, ToDoubleFunction<T> valueFunction, Tag... tags) {
    Gauge.builder(name, state, valueFunction)
        .tags(scopeTags.and(MicrometerTags.of(tags)))
        .register(delegate);
  }

  @Override
  public Timer timer(String name, Tag... tags) {
    return new MicrometerTimer(delegate, name, scopeTags.and(MicrometerTags.of(tags)));
  }

  @Override
  public <T> void asyncCounter(String name, T state, ToLongFunction<T> valueFunction, Tag... tags) {
    FunctionCounter.builder(name, state, valueFunction::applyAsLong)
        .tags(scopeTags.and(MicrometerTags.of(tags)))
        .register(delegate);
  }
}

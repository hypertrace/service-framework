package org.hypertrace.core.serviceframework.metrics.v2.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.hypertrace.core.serviceframework.metrics.v2.Counter;
import org.hypertrace.core.serviceframework.metrics.v2.Tag;

/**
 * Micrometer-backed {@link Counter}. Underlying Micrometer counters are resolved lazily per runtime
 * tag set and cached, so repeated increments with the same tag values reuse the same underlying
 * counter.
 *
 * <p>No counter is registered until the first increment. This matters because Prometheus (and hence
 * Micrometer's Prometheus registry) requires every series sharing a metric name to carry the same
 * tag keys; registering an untagged base counter up front would fix the key set without the runtime
 * tag and cause every subsequent {@link #increment(long, Tag...)} call to be rejected. Callers must
 * therefore be consistent for a given metric name: either always increment with the same set of tag
 * keys, or always increment untagged.
 */
final class MicrometerCounter implements Counter {
  private final MeterRegistry registry;
  private final String name;
  private final Tags baseTags;
  private final ConcurrentMap<List<Tag>, io.micrometer.core.instrument.Counter> counters;

  MicrometerCounter(MeterRegistry registry, String name, Tags baseTags) {
    this.registry = registry;
    this.name = name;
    this.baseTags = baseTags;
    this.counters = new ConcurrentHashMap<>();
  }

  @Override
  public void increment(long amount, Tag... tags) {
    counters
        .computeIfAbsent(
            List.of(tags), k -> registry.counter(name, baseTags.and(MicrometerTags.of(tags))))
        .increment(amount);
  }
}

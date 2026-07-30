package org.hypertrace.core.serviceframework.metrics.v2.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.hypertrace.core.serviceframework.metrics.v2.Tag;
import org.hypertrace.core.serviceframework.metrics.v2.Timer;

/**
 * Micrometer-backed {@link Timer}. Underlying Micrometer timers are resolved lazily per runtime tag
 * set and cached, so repeated recordings with the same tag values reuse the same underlying timer.
 *
 * <p>No timer is registered until the first recording. This matters because Prometheus (and hence
 * Micrometer's Prometheus registry) requires every series sharing a metric name to carry the same
 * tag keys; registering an untagged base timer up front would fix the key set without the runtime
 * tag and cause every subsequent {@link #record(Duration, Tag...)} call to be rejected. Callers
 * must therefore be consistent for a given metric name: either always record with the same set of
 * tag keys, or always record untagged.
 */
final class MicrometerTimer implements Timer {
  private final MeterRegistry registry;
  private final String name;
  private final Tags baseTags;
  private final ConcurrentMap<List<Tag>, io.micrometer.core.instrument.Timer> timers;

  MicrometerTimer(MeterRegistry registry, String name, Tags baseTags) {
    this.registry = registry;
    this.name = name;
    this.baseTags = baseTags;
    this.timers = new ConcurrentHashMap<>();
  }

  @Override
  public void record(Duration duration, Tag... tags) {
    timers
        .computeIfAbsent(
            List.of(tags), k -> registry.timer(name, baseTags.and(MicrometerTags.of(tags))))
        .record(duration);
  }
}

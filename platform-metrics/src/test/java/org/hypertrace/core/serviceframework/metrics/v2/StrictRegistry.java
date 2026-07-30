package org.hypertrace.core.serviceframework.metrics.v2;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

/**
 * Test {@link MetricRegistry} that throws on a duplicate {@code (name, tags)} registration,
 * modeling strict backends that reject registering the same meter twice. Useful for asserting that
 * callers register each meter at most once. Returned meters are shared no-ops.
 */
public final class StrictRegistry implements MetricRegistry {
  private static final Counter NOOP_COUNTER =
      new Counter() {
        @Override
        public void increment(long amount, Tag... tags) {
          // no-op
        }
      };
  private static final Timer NOOP_TIMER =
      new Timer() {
        @Override
        public void record(Duration duration, Tag... tags) {
          // no-op
        }
      };

  private final Set<String> registered = new HashSet<>();

  @Override
  public Counter counter(String name, Tag... tags) {
    register(name, tags);
    return NOOP_COUNTER;
  }

  @Override
  public Timer timer(String name, Tag... tags) {
    register(name, tags);
    return NOOP_TIMER;
  }

  @Override
  public <T> void gauge(String name, T state, ToDoubleFunction<T> valueFunction, Tag... tags) {
    register(name, tags);
  }

  @Override
  public <T> void asyncCounter(String name, T state, ToLongFunction<T> valueFunction, Tag... tags) {
    register(name, tags);
  }

  private void register(String name, Tag... tags) {
    if (!registered.add(name + Arrays.toString(tags))) {
      throw new IllegalStateException("Duplicate metric registration: " + name);
    }
  }
}

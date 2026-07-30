package org.hypertrace.core.serviceframework.metrics.v2;

import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

/**
 * Creates named metrics scoped by a set of tags.
 *
 * <p>Tag ordering is the caller's responsibility. A metric's identity is derived from its tags in
 * the order supplied, so callers must pass the same tags in a consistent order to address the same
 * metric; differently ordered tags may resolve to distinct time series.
 */
public interface MetricRegistry {
  /**
   * Returns a counter for {@code name}. The given static tags are bound to every increment; further
   * per-increment dimensions can be supplied via {@link Counter#increment(long, Tag...)}.
   *
   * @param name counter name
   * @param tags static dimensions shared by every increment of this counter
   */
  Counter counter(String name, Tag... tags);

  /**
   * Registers an observed gauge for {@code name}. The metrics backend samples {@code state} via
   * {@code valueFunction} to read the current value; use for quantities derived from external state
   * (e.g. a cache's size). Register once per name and tag set.
   *
   * <p><strong>Lifetime:</strong> the caller must keep a strong reference to {@code state} for as
   * long as the gauge should report. Backends may hold {@code state} only weakly, so once it
   * becomes unreachable the gauge stops reporting. Pass the durable domain object as {@code state}
   * (e.g. the cache) and a stateless extractor as {@code valueFunction} (e.g. {@code Cache::size})
   * — do not capture the state inside the function.
   *
   * @param name gauge name
   * @param state the object sampled to produce the value; governs the gauge's lifetime
   * @param valueFunction extracts the current value from {@code state} when sampled
   * @param tags static dimensions for this gauge
   * @param <T> the type of the sampled state object
   */
  <T> void gauge(String name, T state, ToDoubleFunction<T> valueFunction, Tag... tags);

  /**
   * Returns a timer for {@code name}. The given static tags are bound to every recording; further
   * per-recording dimensions can be supplied via {@link Timer#record(java.time.Duration, Tag...)}.
   *
   * @param name timer name
   * @param tags static dimensions shared by every recording of this timer
   */
  Timer timer(String name, Tag... tags);

  /**
   * Registers a counter for {@code name} whose value is sampled from external state rather than
   * incremented in-process. The metrics backend reads the current count by applying {@code
   * valueFunction} to {@code state}; use for monotonic totals already tracked elsewhere (e.g. a
   * cache's cumulative hit count). Register once per name and tag set.
   *
   * <p><strong>Lifetime:</strong> the caller must keep a strong reference to {@code state} for as
   * long as the counter should report. Backends may hold {@code state} only weakly, so once it
   * becomes unreachable the counter stops reporting. Pass the durable domain object as {@code
   * state} and a stateless extractor as {@code valueFunction} — do not capture the state inside the
   * function.
   *
   * @param name counter name
   * @param state the object sampled to produce the count; governs the counter's lifetime
   * @param valueFunction extracts the current count from {@code state} when sampled
   * @param tags static dimensions for this counter
   * @param <T> the type of the sampled state object
   */
  <T> void asyncCounter(String name, T state, ToLongFunction<T> valueFunction, Tag... tags);
}

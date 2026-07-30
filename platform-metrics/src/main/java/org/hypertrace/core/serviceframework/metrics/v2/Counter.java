package org.hypertrace.core.serviceframework.metrics.v2;

/**
 * A monotonically increasing counter.
 *
 * <p>Static dimensions common to every increment are bound once via {@link
 * MetricRegistry#counter(String, Tag...)}. Dimensions whose values vary per increment (e.g. a
 * tenant id) are supplied through {@link #increment(long, Tag...)}; the implementation resolves and
 * caches the underlying per-dimension counter, so callers may pass runtime tags directly on the hot
 * path.
 *
 * <p>Tag ordering is the caller's responsibility: runtime tags supplied in a consistent order
 * address the same counter, while differently ordered tags may resolve to distinct time series.
 */
public interface Counter {
  /** Increments the counter by one, using only the static tags bound at creation. */
  default void increment() {
    increment(1L);
  }

  /**
   * Increments the counter by {@code amount}, further scoped by the given runtime tags.
   *
   * @param amount value to add
   * @param tags runtime (per-increment) dimensions layered on top of the static tags
   */
  void increment(long amount, Tag... tags);
}

package org.hypertrace.core.serviceframework.metrics.v2;

import java.time.Duration;

/**
 * Records durations so the metrics backend can expose latency distributions (count, total time, and
 * quantiles such as p95/p99). Static tags supplied when the timer is created are bound to every
 * recording; additional per-recording dimensions can be layered on via {@link #record(Duration,
 * Tag...)}.
 *
 * <p>Tag ordering is the caller's responsibility: runtime tags supplied in a consistent order
 * address the same distribution, while differently ordered tags may resolve to distinct time
 * series.
 */
public interface Timer {
  /**
   * Records a single observed duration, adding the given runtime dimensions on top of this timer's
   * static tags. Recordings that share the same runtime tag values accumulate into the same
   * underlying distribution.
   *
   * @param duration the elapsed time to record
   * @param tags runtime (per-recording) dimensions layered on top of the static tags
   */
  void record(Duration duration, Tag... tags);
}

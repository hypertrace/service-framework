package org.hypertrace.core.serviceframework.metrics;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.hypertrace.core.serviceframework.metrics.PlatformMetricsRegistry.toIterable;

import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.MultiGauge.Row;
import io.micrometer.core.instrument.Tags;
import java.util.Collection;
import java.util.List;

public class ResizeableGauge {
  private final MultiGauge multiGauge;

  ResizeableGauge(final MultiGauge multiGauge) {
    this.multiGauge = multiGauge;
  }

  public void report(final Collection<Measurement> measurements) {
    final List<Row<?>> rows =
        measurements.stream()
            .map(
                measurement ->
                    Row.of(Tags.of(toIterable(measurement.labels())), measurement::value))
            .collect(toUnmodifiableList());

    multiGauge.register(rows);
  }
}

package org.hypertrace.core.serviceframework.metrics;

import java.util.Map;

public class Measurement {
  private final double value;
  private final Map<String, String> labels;

  public Measurement(final double value, final Map<String, String> labels) {
    this.value = value;
    this.labels = labels;
  }

  double value() {
    return value;
  }

  Map<String, String> labels() {
    return labels;
  }
}

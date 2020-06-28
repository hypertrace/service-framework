package org.hypertrace.core.serviceframework.metrics.flink;

import org.apache.flink.metrics.MetricConfig;
import org.apache.flink.metrics.prometheus.AbstractPrometheusReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrometheusReporter extends AbstractPrometheusReporter {
  private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusReporter.class);

  @Override
  public void open(MetricConfig config) {
    LOGGER.info("Open=[{}]", config);
  }
}

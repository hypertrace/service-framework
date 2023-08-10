package org.hypertrace.core.serviceframework.docstore.metrics;

import java.time.Duration;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;
import org.hypertrace.core.documentstore.model.config.CustomMetricConfig;

@Value
@Builder
@Accessors(fluent = true)
public class DocStoreCustomMetricReportingConfig {
  CustomMetricConfig config;
  Duration reportingInterval;
}

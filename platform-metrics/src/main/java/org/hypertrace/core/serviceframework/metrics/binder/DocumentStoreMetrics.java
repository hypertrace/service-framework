package org.hypertrace.core.serviceframework.metrics.binder;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toUnmodifiableList;

import io.micrometer.common.lang.NonNull;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.hypertrace.core.documentstore.Datastore;
import org.hypertrace.core.documentstore.metric.DocStoreMetric;
import org.hypertrace.core.documentstore.metric.DocStoreMetricProvider;
import org.hypertrace.core.documentstore.model.config.CustomMetricConfig;

public class DocumentStoreMetrics implements MeterBinder {
  private final Datastore datastore;
  private final List<CustomMetricReportingConfig> reportingConfigs;
  private final ScheduledExecutorService executors;
  private final Duration connectionCountReportingInterval = Duration.ofDays(1);
  private final long initialDelaySeconds = MINUTES.toSeconds(5);

  public DocumentStoreMetrics(
      final Datastore datastore,
      final List<CustomMetricReportingConfig> reportingConfigs,
      final int numThreadsInPool) {
    this.datastore = datastore;
    this.reportingConfigs = reportingConfigs;
    this.executors = Executors.newScheduledThreadPool(numThreadsInPool);
  }

  @Override
  public void bindTo(@NonNull final MeterRegistry registry) {
    final DocStoreMetricProvider source = datastore.getDocStoreMetricProvider();
    reportCustomMetrics(source, registry);
    reportConnectionCountMetric(source, registry);
  }

  private void reportCustomMetrics(
      final DocStoreMetricProvider source, final MeterRegistry registry) {
    reportingConfigs.forEach(
        reportingConfig ->
            executors.scheduleAtFixedRate(
                () ->
                    source
                        .getCustomMetrics(reportingConfig.config)
                        .forEach(metric -> report(metric, registry)),
                initialDelaySeconds,
                reportingConfig.reportingInterval.toSeconds(),
                SECONDS));
  }

  private void reportConnectionCountMetric(
      final DocStoreMetricProvider source, final MeterRegistry registry) {
    executors.scheduleAtFixedRate(
        () -> report(source.getConnectionCountMetric(), registry),
        initialDelaySeconds,
        connectionCountReportingInterval.toSeconds(),
        SECONDS);
  }

  private void report(final DocStoreMetric metric, final MeterRegistry registry) {
    final Tags tags =
        metric.labels().entrySet().stream()
            .map(entry -> Tag.of(entry.getKey(), entry.getValue()))
            .collect(collectingAndThen(toUnmodifiableList(), Tags::of));
    Gauge.builder(metric.name(), metric::value).tags(tags).register(registry);
  }

  public static class CustomMetricReportingConfig {
    private final CustomMetricConfig config;
    private final Duration reportingInterval;

    public CustomMetricReportingConfig(
        final CustomMetricConfig config, final Duration reportingInterval) {
      this.config = config;
      this.reportingInterval = reportingInterval;
    }
  }
}

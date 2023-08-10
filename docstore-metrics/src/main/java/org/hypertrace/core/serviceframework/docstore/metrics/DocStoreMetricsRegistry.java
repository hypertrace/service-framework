package org.hypertrace.core.serviceframework.docstore.metrics;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.hypertrace.core.documentstore.Datastore;
import org.hypertrace.core.documentstore.metric.DocStoreMetric;
import org.hypertrace.core.documentstore.metric.DocStoreMetricProvider;
import org.hypertrace.core.serviceframework.metrics.PlatformMetricsRegistry;

@SuppressWarnings("unused")
public class DocStoreMetricsRegistry {
  private static final Duration connectionCountReportingInterval = Duration.ofDays(1);
  private static final long initialDelaySeconds = MINUTES.toSeconds(5);

  public static ScheduledExecutorService monitor(
      final Datastore datastore,
      final List<DocStoreCustomMetricReportingConfig> reportingConfigs,
      final int threadPoolSize) {
    final DocStoreMetricProvider source = datastore.getDocStoreMetricProvider();
    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(threadPoolSize);
    monitorCustomMetrics(source, reportingConfigs, executor);
    monitorConnectionCountMetric(source.getConnectionCountMetric(), executor);
    return executor;
  }

  private static void monitorCustomMetrics(
      final DocStoreMetricProvider source,
      final List<DocStoreCustomMetricReportingConfig> reportingConfigs,
      final ScheduledExecutorService executor) {
    reportingConfigs.forEach(
        reportingConfig ->
            executor.scheduleAtFixedRate(
                () ->
                    source
                        .getCustomMetrics(reportingConfig.config())
                        .forEach(DocStoreMetricsRegistry::report),
                initialDelaySeconds,
                reportingConfig.reportingInterval().toSeconds(),
                SECONDS));
  }

  private static void monitorConnectionCountMetric(
      final DocStoreMetric connectionCountMetric, final ScheduledExecutorService executor) {
    executor.scheduleAtFixedRate(
        () -> report(connectionCountMetric),
        initialDelaySeconds,
        connectionCountReportingInterval.toSeconds(),
        SECONDS);
  }

  private static void report(final DocStoreMetric metric) {
    PlatformMetricsRegistry.registerGauge(metric.name(), metric.labels(), metric.value());
  }
}

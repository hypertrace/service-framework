package org.hypertrace.core.serviceframework.docstore.metrics;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import org.hypertrace.core.documentstore.Datastore;
import org.hypertrace.core.documentstore.metric.DocStoreMetric;
import org.hypertrace.core.documentstore.metric.DocStoreMetricProvider;
import org.hypertrace.core.documentstore.model.config.CustomMetricConfig;
import org.hypertrace.core.serviceframework.metrics.PlatformMetricsRegistry;

@SuppressWarnings("unused")
public class DocStoreMetricsRegistry {
  private static final long initialDelaySeconds = MINUTES.toSeconds(5);

  /**
   * To continuously monitor a database and periodically report (standard and custom) metrics.
   *
   * <p>The standard metrics (like the database connection count this service is holding) are
   * reported immediately after this method is invoked and subsequently reported once in every 24
   * hours.
   *
   * <p>The custom metrics are reported at the interval scheduled per metric
   *
   * @param datastore The datastore to be monitored
   * @param reportingConfigs The custom metric configurations to be reported, if any
   * @param threadPoolSize The number of threads to be allocated for scheduling. Can just be 1 in
   *     most cases
   * @return The created scheduled executor pool
   */
  public static ScheduledExecutorService monitor(
      final Datastore datastore,
      final List<DocStoreCustomMetricReportingConfig> reportingConfigs,
      final int threadPoolSize) {
    final DocStoreMetricProvider source = datastore.getDocStoreMetricProvider();
    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(threadPoolSize);

    new StandardDocStoreMetricsRegistry(source, executor).monitor();
    monitorCustomMetrics(datastore, reportingConfigs, executor);

    return executor;
  }

  /**
   * Instantly query the datastore and report the custom metric once
   *
   * @param datastore The datastore to be queried
   * @param customMetricConfig The custom metric configuration to be reported
   */
  public static void report(
      final Datastore datastore, final CustomMetricConfig customMetricConfig) {
    final DocStoreMetricProvider source = datastore.getDocStoreMetricProvider();
    source.getCustomMetrics(customMetricConfig).forEach(DocStoreMetricsRegistry::report);
  }

  private static void monitorCustomMetrics(
      final Datastore datastore,
      final List<DocStoreCustomMetricReportingConfig> reportingConfigs,
      final ScheduledExecutorService executor) {
    reportingConfigs.forEach(
        reportingConfig ->
            executor.scheduleAtFixedRate(
                () -> report(datastore, reportingConfig.config()),
                initialDelaySeconds,
                reportingConfig.reportingInterval().toSeconds(),
                SECONDS));
  }

  private static void report(final DocStoreMetric metric) {
    PlatformMetricsRegistry.registerGauge(metric.name(), metric.labels(), metric.value());
  }

  private static class StandardDocStoreMetricsRegistry {
    private static final Duration dbMetricReportingInterval = Duration.ofDays(1);

    private final DocStoreMetricProvider source;
    private final ScheduledExecutorService executor;
    private final AtomicLong connectionCount;

    public StandardDocStoreMetricsRegistry(
        final DocStoreMetricProvider source, final ScheduledExecutorService executor) {
      this.source = source;
      this.executor = executor;

      this.connectionCount = registerConnectionCountMetric();
    }

    private void monitor() {
      executor.scheduleAtFixedRate(
          this::queryDocStoreAndSetMetricValues,
          initialDelaySeconds,
          dbMetricReportingInterval.toSeconds(),
          SECONDS);
    }

    private AtomicLong registerConnectionCountMetric() {
      final DocStoreMetric docStoreMetric = source.getConnectionCountMetric();
      return PlatformMetricsRegistry.registerGauge(
          docStoreMetric.name(),
          docStoreMetric.labels(),
          new AtomicLong(castToLong(docStoreMetric.value())));
    }

    private void queryDocStoreAndSetMetricValues() {
      connectionCount.set(castToLong(source.getConnectionCountMetric().value()));
    }

    private long castToLong(final double value) {
      return Double.valueOf(value).longValue();
    }
  }
}

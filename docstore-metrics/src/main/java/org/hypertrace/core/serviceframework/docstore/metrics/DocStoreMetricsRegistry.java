package org.hypertrace.core.serviceframework.docstore.metrics;

import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.hypertrace.core.serviceframework.metrics.PlatformMetricsRegistry.toIterable;

import io.micrometer.common.lang.Nullable;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.MultiGauge.Row;
import io.micrometer.core.instrument.Tags;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hypertrace.core.documentstore.Datastore;
import org.hypertrace.core.documentstore.metric.DocStoreMetric;
import org.hypertrace.core.documentstore.metric.DocStoreMetricProvider;
import org.hypertrace.core.serviceframework.metrics.PlatformMetricsRegistry;
import org.hypertrace.core.serviceframework.spi.PlatformServiceLifecycle;

@Slf4j
@SuppressWarnings("unused")
public class DocStoreMetricsRegistry {
  private static final long INITIAL_DELAY_SECONDS = MINUTES.toSeconds(5);

  private final DocStoreMetricProvider metricProvider;
  @Nullable private PlatformServiceLifecycle platformLifecycle;
  private int threadPoolSize;
  private List<DocStoreCustomMetricReportingConfig> customMetricConfigs;
  private Duration standardMetricsReportingInterval;
  private ScheduledExecutorService executor;

  public DocStoreMetricsRegistry(@NonNull final Datastore datastore) {
    metricProvider = datastore.getDocStoreMetricProvider();
    platformLifecycle = null;
    threadPoolSize = 1;
    customMetricConfigs = emptyList();
    standardMetricsReportingInterval = Duration.ofMinutes(30);
  }

  /**
   * Supply the platform lifecycle to stop monitoring the datastore when the service is shutting
   * down
   */
  public DocStoreMetricsRegistry withPlatformLifecycle(
      final PlatformServiceLifecycle platformLifecycle) {
    this.platformLifecycle = platformLifecycle;
    return this;
  }

  /** Define the custom metrics to be reported */
  public DocStoreMetricsRegistry withCustomMetrics(
      @NonNull final List<DocStoreCustomMetricReportingConfig> customMetricConfigs) {
    this.customMetricConfigs = customMetricConfigs;
    return this;
  }

  /**
   * Override the number of threads dedicated for datastore metric reporting. The default value is
   * 1.
   */
  public DocStoreMetricsRegistry withThreadPoolSize(final int threadPoolSize) {
    this.threadPoolSize = threadPoolSize;
    return this;
  }

  /**
   * Override the reporting interval of the standard datastore metrics. The default value is 30
   * minutes.
   */
  public DocStoreMetricsRegistry withStandardMetricsReportingInterval(
      @NonNull final Duration standardMetricsReportingInterval) {
    this.standardMetricsReportingInterval = standardMetricsReportingInterval;
    return this;
  }

  /**
   * Continuously monitor a database and periodically report (standard and custom) metrics.
   *
   * <p>The standard metrics (like the database connection count this service is holding) are
   * reported immediately after this method is invoked and subsequently reported at the standard
   * metrics reporting interval (configurable using {@link #withStandardMetricsReportingInterval})
   *
   * <p>The custom metrics, provided through {@link #withCustomMetrics}, are reported immediately
   * after this method is invoked and subsequently reported at the interval scheduled per metric.
   */
  public void monitor() {
    shutdown();
    executor = Executors.newScheduledThreadPool(threadPoolSize);

    addShutdownHook();

    new StandardDocStoreMetricsRegistry().monitor();
    monitorCustomMetrics();
  }

  /** Stop monitoring the database */
  public void shutdown() {
    if (executor != null) {
      executor.shutdown();
    }
  }

  private void addShutdownHook() {
    if (platformLifecycle != null) {
      platformLifecycle.shutdownComplete().thenRun(this::shutdown);
    }
  }

  private void monitorCustomMetrics() {
    customMetricConfigs.forEach(
        reportingConfig -> {
          final MultiGauge multiGauge =
              PlatformMetricsRegistry.registerMultiGauge(reportingConfig.config().metricName());
          executor.scheduleAtFixedRate(
              () -> report(reportingConfig, multiGauge),
              INITIAL_DELAY_SECONDS,
              reportingConfig.reportingInterval().toSeconds(),
              SECONDS);
        });
  }

  private void report(
      final DocStoreCustomMetricReportingConfig reportingConfig, final MultiGauge multiGauge) {
    final List<DocStoreMetric> customMetrics =
        metricProvider.getCustomMetrics(reportingConfig.config());

    log.debug(
        "Reporting custom database metrics {} for configuration {}",
        customMetrics,
        reportingConfig);

    final List<Row<?>> rows =
        customMetrics.stream()
            .map(metric -> Row.of(Tags.of(toIterable(metric.labels())), metric::value))
            .collect(toUnmodifiableList());

    multiGauge.register(rows);
  }

  private class StandardDocStoreMetricsRegistry {
    private final AtomicLong connectionCount;

    public StandardDocStoreMetricsRegistry() {
      this.connectionCount = registerConnectionCountMetric();
    }

    private void monitor() {
      executor.scheduleAtFixedRate(
          this::queryDocStoreAndSetMetricValues,
          INITIAL_DELAY_SECONDS,
          standardMetricsReportingInterval.toSeconds(),
          SECONDS);
    }

    private AtomicLong registerConnectionCountMetric() {
      final DocStoreMetric docStoreMetric = metricProvider.getConnectionCountMetric();
      return PlatformMetricsRegistry.registerGauge(
          docStoreMetric.name(),
          docStoreMetric.labels(),
          new AtomicLong(castToLong(docStoreMetric.value())));
    }

    private void queryDocStoreAndSetMetricValues() {
      connectionCount.set(castToLong(metricProvider.getConnectionCountMetric().value()));
    }

    private long castToLong(final double value) {
      return Double.valueOf(value).longValue();
    }
  }
}

package org.hypertrace.core.serviceframework.metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.JvmAttributeGaugeSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PlatformMetricsRegistry is the MetricRegistry used for all the metrics. Framework will take care
 * of metrics exporters.
 */
public class PlatformMetricsRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(PlatformMetricsRegistry.class);
  public static final int METRICS_REPORTER_CONSOLE_REPORT_INTERVAL_DEFAULT = 30;
  public static final String DEFAULT_METRICS_PREFIX = "org.hypertrace.core.serviceframework";


  private static final MetricRegistry METRIC_REGISTRY = new MetricRegistry();
  private static ConsoleReporter consoleReporter;
  private static String metricsPrefix;
  public static final List<String> DEFAULT_METRICS_REPORTERS = List.of("prometheus");
  private static final Map<String, MetricSet> DEFAULT_METRIC_SET = new HashMap<>() {{
    put("gc", new GarbageCollectorMetricSet());
    put("jvm", new JvmAttributeGaugeSet());
    put("memory", new MemoryUsageGaugeSet());
    put("thread", new ThreadStatesGaugeSet());
  }};
  private static boolean isInit = false;

  private static void initPrometheusExporter() {
    LOGGER.info("Trying to init PrometheusReporter");
    CollectorRegistry.defaultRegistry.register(new DropwizardExports(METRIC_REGISTRY));
  }

  private static void initConsoleMetricsReporter() {
    initConsoleMetricsReporter(METRICS_REPORTER_CONSOLE_REPORT_INTERVAL_DEFAULT);
  }

  private static void initConsoleMetricsReporter(final int reportInterval) {
    consoleReporter = ConsoleReporter.forRegistry(METRIC_REGISTRY).build();
    LOGGER
        .info("Trying to init ConsoleReporter with reporter interval=[{}] seconds", reportInterval);
    consoleReporter.start(reportInterval, TimeUnit.SECONDS);
  }

  public synchronized static void initMetricsRegistry() {
    initMetricsRegistry(DEFAULT_METRICS_REPORTERS, DEFAULT_METRICS_PREFIX,
        METRICS_REPORTER_CONSOLE_REPORT_INTERVAL_DEFAULT);
  }

  public synchronized static void initMetricsRegistry(final List<String> reporters,
      final String prefix,
      final int reportInterval) {
    if (isInit) {
      return;
    }

    metricsPrefix = prefix;

    for (String reporter : reporters) {

      switch (reporter.toLowerCase()) {
        case "console":
          initConsoleMetricsReporter(reportInterval);
          break;
        case "prometheus":
          initPrometheusExporter();
          break;
        default:
          LOGGER.warn("Cannot find metric reporter: {}", reporter);
      }
    }
    for (String key : DEFAULT_METRIC_SET.keySet()) {
      METRIC_REGISTRY
          .registerAll(String.format("%s.%s", metricsPrefix, key), DEFAULT_METRIC_SET.get(key));
    }
    isInit = true;
  }

  public static void register(String metricName, Metric metric) {
    final String fullMetricName = String.format("%s.%s", metricsPrefix, metricName);
    if (!METRIC_REGISTRY.getNames().contains(fullMetricName)) {
      METRIC_REGISTRY.register(fullMetricName, metric);
    }
  }

  public static MetricRegistry getMetricRegistry() {
    return METRIC_REGISTRY;
  }

  public static void stop() {
    stopConsoleMetricsReporter();
  }

  private static void stopConsoleMetricsReporter() {
    if (consoleReporter == null) {
      return;
    }
    // report all metrics one final time
    consoleReporter.report();
    // stop console reporter
    consoleReporter.stop();
  }
}

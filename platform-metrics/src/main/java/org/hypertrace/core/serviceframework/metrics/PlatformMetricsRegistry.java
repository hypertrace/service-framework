package org.hypertrace.core.serviceframework.metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.JvmAttributeGaugeSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.google.common.cache.Cache;
import com.typesafe.config.Config;
import io.github.mweirauch.micrometer.jvm.extras.ProcessMemoryMetrics;
import io.github.mweirauch.micrometer.jvm.extras.ProcessThreadMetrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.cache.GuavaCacheMetrics;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.util.StringUtils;
import io.micrometer.core.lang.NonNull;
import io.micrometer.core.lang.Nullable;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.PushGateway;
import org.hypertrace.core.serviceframework.metrics.config.PrometheusPushRegistryConfig;
import org.hypertrace.core.serviceframework.metrics.registry.PrometheusPushMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * PlatformMetricsRegistry is the MetricRegistry used for all the metrics. Framework will take care
 * of metrics exporters.
 */
public class PlatformMetricsRegistry {
  private static final Logger LOGGER = LoggerFactory.getLogger(PlatformMetricsRegistry.class);

  public static final String DEFAULT_METRICS_PREFIX = "org.hypertrace.core.serviceframework";
  private static final int DEFAULT_METRIC_REPORT_INTERVAL_SEC = 30;

  private static final String METRICS_REPORTER_NAMES_CONFIG_KEY = "reporter.names";
  private static final String METRICS_REPORTER_PREFIX_CONFIG_KEY = "reporter.prefix";
  private static final String METRICS_REPORT_INTERVAL_CONFIG_KEY = "reportInterval";
  private static final String METRICS_REPORT_PUSH_URL_ADDRESS = "pushUrlAddress";
  private static final String PROMETHEUS_REPORTER_NAME = "prometheus";
  private static final String PUSH_GATEWAY_REPORTER_NAME = "pushgateway";
  private static final String LOGGING_REPORTER_NAME = "logging";
  private static final String TESTING_REPORTER_NAME = "testing";
  private static final String CONSOLE_REPORTER_NAME = "console";

  /**
   * List of tags that need to be reported for all the metrics reported by this service. The tags
   * are given as a list with tag key followed by corresponding value. Any key without a value will
   * be ignored. Example: defaultTags = ["k1", "v1", "k2", "v2"]
   *
   * <p>Please note "app:serviceName" will be reported by default for all metrics, and hence needn't
   * be included in this list.
   */
  private static final String METRICS_DEFAULT_TAGS_CONFIG_KEY = "defaultTags";

  private static final MetricRegistry METRIC_REGISTRY = new MetricRegistry();
  private static ConsoleReporter consoleReporter;
  private static String metricsPrefix;
  public static final List<String> DEFAULT_METRICS_REPORTERS = List.of("prometheus");
  private static final Map<String, MetricSet> DEFAULT_METRIC_SET =
      new HashMap<>() {
        {
          put("gc", new GarbageCollectorMetricSet());
          put("jvm", new JvmAttributeGaugeSet());
          put("memory", new MemoryUsageGaugeSet());
          put("thread", new ThreadStatesGaugeSet());
        }
      };
  private static boolean isInit = false;
  private static final Set<Tag> DEFAULT_TAGS = new HashSet<>();

  /**
   * Main MetricMeter registry, with which all the metrics should be registered. We use a {@link
   * CompositeMeterRegistry} here so that we can even registry multiple registries like Prometheus
   * and Logging registries, if needed.
   */
  private static final CompositeMeterRegistry METER_REGISTRY = new CompositeMeterRegistry();

  private static void initPrometheusReporter(int reportInterval) {
    LOGGER.info("Trying to init PrometheusReporter");

    // Add Prometheus registry to the composite registry.
    METER_REGISTRY.add(
        new PrometheusMeterRegistry(
            new PrometheusConfig() {
              @Override
              @NonNull
              public Duration step() {
                return Duration.ofSeconds(reportInterval);
              }

              @Override
              @io.micrometer.core.lang.Nullable
              public String get(String k) {
                return null;
              }
            },
            CollectorRegistry.defaultRegistry,
            Clock.SYSTEM));

    CollectorRegistry.defaultRegistry.register(new DropwizardExports(METRIC_REGISTRY));
  }

  private static void initConsoleMetricsReporter(final int reportIntervalSec) {
    consoleReporter = ConsoleReporter.forRegistry(METRIC_REGISTRY).build();
    LOGGER.info(
        "Trying to init ConsoleReporter with reporter interval=[{}] seconds", reportIntervalSec);
    consoleReporter.start(reportIntervalSec, TimeUnit.SECONDS);
  }

  private static void initLoggingMetricsReporter(int reportIntervalSec) {
    LOGGER.info("Initializing the logging metric reporter.");

    METER_REGISTRY.add(
        new LoggingMeterRegistry(
            new LoggingRegistryConfig() {
              @Override
              @NonNull
              public Duration step() {
                return Duration.ofSeconds(reportIntervalSec);
              }

              @Override
              @io.micrometer.core.lang.Nullable
              public String get(String key) {
                return null;
              }
            },
            Clock.SYSTEM));
  }

  private static void initTestingMetricsReporter() {
    LOGGER.info("Initializing the testing metric reporter.");

    METER_REGISTRY.add(new SimpleMeterRegistry());
  }

  private static void initPrometheusPushGatewayReporter(
      String serviceName, int reportIntervalSec, String pushUrlAddress) {
    LOGGER.info(
        "Initializing Prometheus PushGateway Reporter with urlAddress: {}, jobName: {}. "
            + "Metric is configured get pushed for every {} seconds",
        pushUrlAddress,
        serviceName,
        reportIntervalSec);

    if (pushUrlAddress == null || pushUrlAddress.isEmpty()) {
      throw new IllegalArgumentException("pushUrlAddress configuration is not specified.");
    }

    METER_REGISTRY.add(
        new PrometheusPushMeterRegistry(
            new PrometheusPushRegistryConfig() {
              @Override
              public String jobName() {
                return serviceName;
              }

              @Override
              public String prefix() {
                return PUSH_GATEWAY_REPORTER_NAME;
              }

              @Override
              @io.micrometer.core.lang.Nullable
              public String get(String key) {
                return null;
              }

              @Override
              public Duration step() {
                return Duration.ofSeconds(reportIntervalSec);
              }
            },
            Executors.defaultThreadFactory(),
            new PushGateway(pushUrlAddress)));
  }

  private static List<String> getStringList(Config config, String path, List<String> defaultVal) {
    if (config.hasPath(path)) {
      return config.getStringList(path);
    }
    return defaultVal;
  }

  public static synchronized void initMetricsRegistry(String serviceName, Config config) {
    if (isInit) {
      return;
    }

    validate(config);

    List<String> reporters =
        getStringList(config, METRICS_REPORTER_NAMES_CONFIG_KEY, DEFAULT_METRICS_REPORTERS);

    metricsPrefix = DEFAULT_METRICS_PREFIX;
    if (config.hasPath(METRICS_REPORTER_PREFIX_CONFIG_KEY)) {
      metricsPrefix = config.getString(METRICS_REPORTER_PREFIX_CONFIG_KEY);
    }

    int reportIntervalSec = DEFAULT_METRIC_REPORT_INTERVAL_SEC;
    if (config.hasPath(METRICS_REPORT_INTERVAL_CONFIG_KEY)) {
      reportIntervalSec = config.getInt(METRICS_REPORT_INTERVAL_CONFIG_KEY);
    }

    String pushUrlAddress = null;
    if (config.hasPath(METRICS_REPORT_PUSH_URL_ADDRESS)) {
      pushUrlAddress = config.getString(METRICS_REPORT_PUSH_URL_ADDRESS);
    }
    Map<String, String> defaultTags = new HashMap<>();

    // Add the service name and other given tags to the default tags list.
    if (StringUtils.isNotEmpty(serviceName)) {
      defaultTags.put("app", serviceName);
    }

    List<String> defaultTagsList =
        getStringList(config, METRICS_DEFAULT_TAGS_CONFIG_KEY, List.of());
    for (int i = 0; i + 1 < defaultTagsList.size(); i += 2) {
      defaultTags.put(defaultTagsList.get(i), defaultTagsList.get(i + 1));
    }

    for (String reporter : reporters) {
      switch (reporter.toLowerCase()) {
        case CONSOLE_REPORTER_NAME:
          initConsoleMetricsReporter(reportIntervalSec);
          break;
        case LOGGING_REPORTER_NAME:
          initLoggingMetricsReporter(reportIntervalSec);
          break;
        case PROMETHEUS_REPORTER_NAME:
          initPrometheusReporter(reportIntervalSec);
          break;
        case TESTING_REPORTER_NAME:
          initTestingMetricsReporter();
          break;
        case PUSH_GATEWAY_REPORTER_NAME:
          initPrometheusPushGatewayReporter(serviceName, reportIntervalSec, pushUrlAddress);
          break;
        default:
          LOGGER.warn("Cannot find metric reporter: {}", reporter);
      }
    }

    LOGGER.info("Setting default tags for all metrics to: {}", defaultTags);
    defaultTags.forEach((key, value) -> DEFAULT_TAGS.add(new ImmutableTag(key, value)));

    // Register different metrics with the registry.
    new ClassLoaderMetrics(DEFAULT_TAGS).bindTo(METER_REGISTRY);
    new JvmGcMetrics(DEFAULT_TAGS).bindTo(METER_REGISTRY);
    new ProcessorMetrics(DEFAULT_TAGS).bindTo(METER_REGISTRY);
    new JvmThreadMetrics(DEFAULT_TAGS).bindTo(METER_REGISTRY);
    new JvmMemoryMetrics(DEFAULT_TAGS).bindTo(METER_REGISTRY);
    new UptimeMetrics(DEFAULT_TAGS).bindTo(METER_REGISTRY);

    new ProcessMemoryMetrics().bindTo(METER_REGISTRY);
    new ProcessThreadMetrics().bindTo(METER_REGISTRY);

    for (String key : DEFAULT_METRIC_SET.keySet()) {
      METRIC_REGISTRY.registerAll(
          String.format("%s.%s", metricsPrefix, key), DEFAULT_METRIC_SET.get(key));
    }
    isInit = true;
  }

  /**
   * This method is deprecated since we'll be removing the Dropwizard metrics support in future
   * releases.
   */
  @Deprecated
  public static void register(String metricName, Metric metric) {
    final String fullMetricName = String.format("%s.%s", metricsPrefix, metricName);
    if (!METRIC_REGISTRY.getNames().contains(fullMetricName)) {
      METRIC_REGISTRY.register(fullMetricName, metric);
    }
  }

  /**
   * Registers a Counter with the given name with the service's metric registry and reports it
   * periodically to the configured reporters. Apart from the given tags, the reporting service's
   * default tags also will be reported with the metrics.
   *
   * <p>See https://micrometer.io/docs/concepts#_counters for more details on the Counter.
   */
  public static Counter registerCounter(String name, Map<String, String> tags) {
    return METER_REGISTRY.counter(name, addDefaultTags(tags));
  }

  /**
   * Registers a Timer (without histograms) with the given name with the service's metric registry
   * and reports it periodically to the configured reporters. Apart from the given tags, the
   * reporting service's default tags also will be reported with the metrics.
   *
   * <p>See https://micrometer.io/docs/concepts#_timers for more details on the Timer.
   */
  public static Timer registerTimer(String name, Map<String, String> tags) {
    return registerTimer(name, tags, false);
  }

  /**
   * Registers a Timer with the given name with the service's metric registry and reports it
   * periodically to the configured reporters. Apart from the given tags, the reporting service's
   * default tags also will be reported with the metrics.
   *
   * <p>See https://micrometer.io/docs/concepts#_timers for more details on the Timer.
   */
  public static Timer registerTimer(String name, Map<String, String> tags, boolean histogram) {
    Timer.Builder builder =
        Timer.builder(name).publishPercentiles(0.5, 0.95, 0.99).tags(addDefaultTags(tags));
    if (histogram) {
      builder = builder.publishPercentileHistogram();
    }
    return builder.register(METER_REGISTRY);
  }

  /**
   * Registers the given number as a Gauge with the service's metric registry and reports it
   * periodically to the configured reporters. Apart from the given tags, the reporting service's
   * default tags also will be reported with the metrics.
   *
   * <p>See https://micrometer.io/docs/concepts#_gauges for more details on the Gauges.
   */
  public static <T extends Number> T registerGauge(
      String name, Map<String, String> tags, T number) {
    Gauge.builder(name, number, Number::doubleValue)
        .tags(addDefaultTags(tags))
        .strongReference(true)
        .register(METER_REGISTRY);
    return number;
  }

  /**
   * Registers a DistributionSummary (with predefined percentiles computed locally) for the given
   * name with the service's metric registry and reports it periodically to the configured
   * reporters. Apart from the provided tags, the reporting service's default tags also will be
   * reported with the metrics.
   *
   * <p>See https://micrometer.io/docs/concepts#_distribution_summaries for more details.
   */
  public static DistributionSummary registerDistributionSummary(
      String name, Map<String, String> tags) {
    return registerDistributionSummary(name, tags, false);
  }

  /**
   * Registers a DistributionSummary for the given name with the service's metric registry and
   * reports it periodically to the configured reporters Apart from the provided tags, the reporting
   * service's default tags also will be reported with the metrics.
   *
   * <p>Param histogram – Determines whether percentile histograms should be published.
   *
   * <p>For more details - https://micrometer.io/docs/concepts#_distribution_summaries,
   * https://micrometer.io/docs/concepts#_histograms_and_percentiles
   */
  public static DistributionSummary registerDistributionSummary(
      String name, Map<String, String> tags, boolean histogram) {
    DistributionSummary.Builder builder =
        DistributionSummary.builder(name)
            .publishPercentiles(0.5, 0.95, 0.99)
            .tags(addDefaultTags(tags));
    if (histogram) {
      builder = builder.publishPercentileHistogram();
    }
    return builder.register(METER_REGISTRY);
  }

  /**
   * Registers metrics for GuavaCaches using micrometer's GuavaCacheMetrics under the given
   * cacheName for the given guavaCache
   */
  public static <K, V> Cache<K, V> registerCache(String cacheName, Cache<K, V> guavaCache) {
    Cache<K, V> monitor = GuavaCacheMetrics.monitor(METER_REGISTRY, guavaCache, cacheName);
    return monitor;
  }

  /**
   * Registers metrics for the given executor service with the service's metric registry and reports
   * them periodically to the configured reporters. Apart from the given tags, the reporting
   * service's default tags also will be reported with the metrics.
   *
   * <p>See https://micrometer.io/docs/ref/jvm for more details on the metrics.
   */
  public static void monitorExecutorService(
      String name, ExecutorService executorService, @Nullable Map<String, String> tags) {
    new ExecutorServiceMetrics(executorService, name, addDefaultTags(tags)).bindTo(METER_REGISTRY);
  }

  private static Iterable<Tag> addDefaultTags(Map<String, String> tags) {
    if (tags == null || tags.isEmpty()) {
      return DEFAULT_TAGS;
    }

    Set<Tag> newTags = new HashSet<>(DEFAULT_TAGS);
    tags.forEach((k, v) -> newTags.add(new ImmutableTag(k, v)));
    return newTags;
  }

  public static MetricRegistry getMetricRegistry() {
    return METRIC_REGISTRY;
  }

  public static MeterRegistry getMeterRegistry() {
    return METER_REGISTRY;
  }

  public static synchronized void stop() {
    stopConsoleMetricsReporter();
    METRIC_REGISTRY.getNames().forEach(METRIC_REGISTRY::remove);

    DEFAULT_TAGS.clear();
    /* For each meter registry in this composite, it will call the close function */
    METER_REGISTRY.getRegistries().forEach(MeterRegistry::close);
    METER_REGISTRY.forEachMeter(METER_REGISTRY::remove);
    METER_REGISTRY.getRegistries().forEach(MeterRegistry::clear);
    Set<MeterRegistry> registries = new HashSet<>(METER_REGISTRY.getRegistries());
    registries.forEach(METER_REGISTRY::remove);
    registries.clear();
    CollectorRegistry.defaultRegistry.clear();
    isInit = false;
  }

  /*
   * This is needed because ConsoleMetricReporter.stop() doesn't call report for the last time
   * before closing the scheduled thread
   */
  private static void stopConsoleMetricsReporter() {
    if (consoleReporter == null) {
      return;
    }
    // report all metrics one final time
    consoleReporter.report();
    // stop console reporter
    consoleReporter.stop();
  }

  private static void validate(Config config) {
    List<String> reporters =
        getStringList(config, METRICS_REPORTER_NAMES_CONFIG_KEY, DEFAULT_METRICS_REPORTERS);
    /* can't contain both prometheus pull and push mechanism */
    if (reporters.contains(PROMETHEUS_REPORTER_NAME)
        && reporters.contains(PUSH_GATEWAY_REPORTER_NAME)) {
      throw new IllegalArgumentException(
          "Both prometheus and pushgateway are included in the "
              + METRICS_REPORTER_NAMES_CONFIG_KEY
              + " configuration. Please choose one of them.");
    }
  }
}

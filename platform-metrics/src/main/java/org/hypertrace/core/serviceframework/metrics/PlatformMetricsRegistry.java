package org.hypertrace.core.serviceframework.metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.google.common.cache.Cache;
import com.typesafe.config.Config;
import io.github.mweirauch.micrometer.jvm.extras.ProcessMemoryMetrics;
import io.github.mweirauch.micrometer.jvm.extras.ProcessThreadMetrics;
import io.micrometer.common.util.StringUtils;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.cache.GuavaCacheMetrics;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.Log4j2Metrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.lang.NonNull;
import io.micrometer.core.lang.Nullable;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.PushGateway;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.hypertrace.core.serviceframework.metrics.config.PrometheusPushRegistryConfig;
import org.hypertrace.core.serviceframework.metrics.registry.PrometheusPushMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private static final String CACHE_MAX_SIZE_GAUGE = "cache.max.size";

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
  public static final List<String> DEFAULT_METRICS_REPORTERS = List.of("prometheus");

  private static ConsoleReporter consoleReporter;
  private static String metricsPrefix;
  private static boolean isInit = false;

  /**
   * Main MetricMeter registry, with which all the metrics should be registered. We use a {@link
   * CompositeMeterRegistry} here so that we can even registry multiple registries like Prometheus
   * and Logging registries, if needed.
   */
  private static CompositeMeterRegistry meterRegistry = new CompositeMeterRegistry();

  private static void initPrometheusReporter(int reportInterval) {
    LOGGER.info("Trying to init PrometheusReporter");

    // Add Prometheus registry to the composite registry.
    meterRegistry.add(
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

    meterRegistry.add(
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

    meterRegistry.add(new SimpleMeterRegistry());
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

    meterRegistry.add(
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
    defaultTags.forEach(
        (key, value) -> {
          meterRegistry.config().commonTags(List.of((new ImmutableTag(key, value))));
        });

    // Register different metrics with the registry.

    // JVM metrics
    new JvmInfoMetrics().bindTo(meterRegistry);
    new ClassLoaderMetrics().bindTo(meterRegistry);
    new JvmThreadMetrics().bindTo(meterRegistry);
    new JvmMemoryMetrics().bindTo(meterRegistry);
    new JvmGcMetrics().bindTo(meterRegistry);
    new JvmHeapPressureMetrics().bindTo(meterRegistry);

    // process metrics - micrometer builtin
    new UptimeMetrics().bindTo(meterRegistry);
    new ProcessorMetrics().bindTo(meterRegistry);
    new FileDescriptorMetrics().bindTo(meterRegistry);

    // logging metrics
    new Log4j2Metrics().bindTo(meterRegistry);

    // process metrics - third party
    new ProcessMemoryMetrics().bindTo(meterRegistry);
    new ProcessThreadMetrics().bindTo(meterRegistry);

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
    return meterRegistry.counter(name, toIterable(tags));
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
   *
   * <p>Defaults the timer range from 1 second to 60 seconds (both inclusive)
   */
  public static Timer registerTimer(String name, Map<String, String> tags, boolean histogram) {
    return registerTimer(name, tags, histogram, Duration.ofSeconds(1), Duration.ofSeconds(60));
  }

  /**
   * Registers a Timer with the given name with the service's metric registry and reports it
   * periodically to the configured reporters. Apart from the given tags, the reporting service's
   * default tags also will be reported with the metrics.
   *
   * <p>See https://micrometer.io/docs/concepts#_timers for more details on the Timer.
   */
  public static Timer registerTimer(
      String name,
      Map<String, String> tags,
      boolean histogram,
      Duration minExpectedValue,
      Duration maxExpectedValue) {
    Timer.Builder builder =
        Timer.builder(name).publishPercentiles(0.5, 0.75, 0.90, 0.95, 0.99).tags(toIterable(tags));

    if (histogram) {
      builder =
          builder
              .minimumExpectedValue(minExpectedValue)
              .maximumExpectedValue(maxExpectedValue)
              .publishPercentileHistogram();
    }
    return builder.register(meterRegistry);
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
        .tags(toIterable(tags))
        .strongReference(true)
        .register(meterRegistry);
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
    return registerDistributionSummary(name, tags, histogram, null, null);
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
      String name,
      Map<String, String> tags,
      boolean histogram,
      Double minExpectedValue,
      Double maxExpectedValue) {
    DistributionSummary.Builder builder =
        DistributionSummary.builder(name)
            .publishPercentiles(0.5, 0.75, 0.90, 0.95, 0.99)
            .tags(toIterable(tags));
    if (histogram) {
      builder =
          builder
              .minimumExpectedValue(minExpectedValue)
              .maximumExpectedValue(maxExpectedValue)
              .publishPercentileHistogram();
    }
    return builder.register(meterRegistry);
  }

  /**
   * Registers metrics for GuavaCaches using micrometer's GuavaCacheMetrics under the given
   * cacheName for the given guavaCache
   */
  public static <K, V> void registerCache(
      String cacheName, Cache<K, V> guavaCache, Map<String, String> tags) {
    GuavaCacheMetrics.monitor(meterRegistry, guavaCache, cacheName, toIterable(tags));
  }

  /**
   * Registers metrics for GuavaCaches using micrometer's GuavaCacheMetrics under the given
   * cacheName for the given guavaCache and also reports maximum size configured
   */
  public static <K, V> void registerCacheTrackingOccupancy(
      String cacheName, Cache<K, V> guavaCache, Map<String, String> tags, long maxSize) {
    GuavaCacheMetrics.monitor(meterRegistry, guavaCache, cacheName, toIterable(tags));
    Map<String, String> tagsForGauge = new HashMap<>(tags);
    tagsForGauge.put("cache", cacheName);
    registerGauge(CACHE_MAX_SIZE_GAUGE, tagsForGauge, maxSize);
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
    new ExecutorServiceMetrics(executorService, name, toIterable(tags)).bindTo(meterRegistry);
  }

  public static MetricRegistry getMetricRegistry() {
    return METRIC_REGISTRY;
  }

  public static MeterRegistry getMeterRegistry() {
    return meterRegistry;
  }

  public static synchronized void stop() {
    stopConsoleMetricsReporter();
    METRIC_REGISTRY.getNames().forEach(METRIC_REGISTRY::remove);

    /* For each meter registry in this composite, it will call the close function */
    meterRegistry.getRegistries().forEach(MeterRegistry::close);
    meterRegistry.forEachMeter(meterRegistry::remove);
    meterRegistry.getRegistries().forEach(MeterRegistry::clear);
    Set<MeterRegistry> registries = new HashSet<>(meterRegistry.getRegistries());
    registries.forEach(meterRegistry::remove);
    registries.clear();
    CollectorRegistry.defaultRegistry.clear();
    meterRegistry = new CompositeMeterRegistry();
    isInit = false;
  }

  public static ResizeableGauge registerResizeableGauge(final String name) {
    return new ResizeableGauge(MultiGauge.builder(name).register(meterRegistry));
  }

  static Iterable<Tag> toIterable(Map<String, String> tags) {
    List<Tag> newTags = new ArrayList<>();

    if (tags != null) {
      tags.forEach((k, v) -> newTags.add(new ImmutableTag(k, v)));
    }

    return newTags;
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

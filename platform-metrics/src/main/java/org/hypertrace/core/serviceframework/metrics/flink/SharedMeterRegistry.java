package org.hypertrace.core.serviceframework.metrics.flink;

import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.IOException;
import java.io.UncheckedIOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hypertrace.core.serviceframework.metrics.PlatformMetricsRegistry;

/**
 * Lazily initializes the process-wide Prometheus scrape endpoint used by Flink TaskManagers.
 *
 * <p>Application meters still report through {@link PlatformMetricsRegistry#getMeterRegistry()};
 * this class only binds the HTTP scrape server and exposes the underlying {@link
 * PrometheusRegistry} so Flink's metric reporter can bridge Flink-native metrics onto the same
 * endpoint. The exporter is started exactly once per JVM: the port from the first caller wins, and
 * later callers reuse that instance.
 *
 * <p>Parameters are primitives / {@link String} on purpose so this class can sit on Flink's
 * parent-first shared classpath without coupling that surface to typesafe-config.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SharedMeterRegistry {

  private static boolean initialized = false;

  /**
   * Returns the process-wide Prometheus registry, starting {@link PlatformMetricsRegistry} and the
   * scrape-endpoint HTTP server on first use.
   *
   * @param serviceName service name added to the common metric tags
   * @param exporterPort the port on which to publish the Prometheus scrape endpoint
   * @return the shared Prometheus registry
   */
  public static synchronized PrometheusRegistry getPrometheusRegistry(
      String serviceName, int exporterPort) {
    if (!initialized) {
      PlatformMetricsRegistry.initMetricsRegistry(serviceName);
      try {
        HTTPServer.builder()
            .port(exporterPort)
            .registry(PlatformMetricsRegistry.getPrometheusRegistry())
            .buildAndStart();
      } catch (IOException e) {
        throw new UncheckedIOException("Failed to start Prometheus exporter HTTP server", e);
      }
      initialized = true;
    }
    return PlatformMetricsRegistry.getPrometheusRegistry();
  }
}

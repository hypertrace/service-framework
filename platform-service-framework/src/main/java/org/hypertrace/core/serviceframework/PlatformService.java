package org.hypertrace.core.serviceframework;

import com.codahale.metrics.servlets.CpuProfileServlet;
import com.codahale.metrics.servlets.ThreadDumpServlet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.core.serviceframework.config.ConfigClientFactory;
import org.hypertrace.core.serviceframework.metrics.MetricsServlet;
import org.hypertrace.core.serviceframework.metrics.PlatformMetricsRegistry;
import org.hypertrace.core.serviceframework.service.servlets.HealthCheckServlet;
import org.hypertrace.core.serviceframework.service.servlets.JVMDiagnosticServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PlatformService {
  private static final Logger LOGGER = LoggerFactory.getLogger(PlatformService.class);

  private static final String METRICS_CONFIG_KEY = "metrics";

  static {
    try {
      // System property: hostname is used by kafka logger for log aggregation.
      System.setProperty("hostname", InetAddress.getLocalHost().getHostName());
    } catch (UnknownHostException e) {
      System.setProperty("hostname", "unknown");
    }
  }

  protected ConfigClient configClient;
  private final Config appConfig;
  private State serviceState = State.NOT_STARTED;
  private Server adminServer;

  public PlatformService() {
    this(ConfigClientFactory.getClient());
  }

  public PlatformService(String bootstrapConfigUri) {
    this(ConfigClientFactory.getClient(bootstrapConfigUri));
  }

  public PlatformService(ConfigClient configClient) {
    this.configClient = configClient;
    this.appConfig = configClient.getConfig();
  }

  // initialize the service. This method will always be called before start.
  protected abstract void doInit();

  // Contains all the logic to start the service.
  protected abstract void doStart();

  // Contains the logic to shutdown service cleanly.
  protected abstract void doStop();

  // Contains the logic to do health check of the service.
  public abstract boolean healthCheck();

  public abstract String getServiceName();

  public State getServiceState() {
    return this.serviceState;
  }

  protected final Config getAppConfig() {
    return this.appConfig;
  }

  public void initialize() {
    if (serviceState != State.NOT_STARTED) {
      LOGGER.info(
          "Service - {} is at state: {}. Expecting state: NOT_STARTED. Skipping initialize...",
          getServiceName(), serviceState);
      return;
    }
    serviceState = State.INITIALIZING;

    LOGGER.info("Starting the service with this config {}", appConfig);
    doInit();

    Config metricsConfig = appConfig.hasPath(METRICS_CONFIG_KEY) ?
        appConfig.getConfig(METRICS_CONFIG_KEY) : ConfigFactory.empty();
    PlatformMetricsRegistry.initMetricsRegistry(getServiceName(), metricsConfig);

    serviceState = State.INITIALIZED;
    LOGGER.info("Service - {} is initialized.", getServiceName());
  }

  public void start() {
    if (serviceState != State.INITIALIZED) {
      LOGGER.info("Service - {} is at state: {}. Expecting state: INITIALIZED. Skipping start...",
          getServiceName(), serviceState);
      return;
    }
    LOGGER.info("Trying to start service - {}...", getServiceName());
    serviceState = State.STARTING;
    final int serviceAdminPort = getServiceAdminPort();
    adminServer = new Server(serviceAdminPort);
    ServletContextHandler context = new ServletContextHandler();
    context.setContextPath("/");
    adminServer.setHandler(context);
    adminServer.setStopAtShutdown(true);
    adminServer.setStopTimeout(2000);

    context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");
    context.addServlet(new ServletHolder(new HealthCheckServlet(this)), "/health");
    context.addServlet(new ServletHolder(new ThreadDumpServlet()), "/threads");
    context.addServlet(new ServletHolder(new CpuProfileServlet()), "/pprof");
    context.addServlet(new ServletHolder(new JVMDiagnosticServlet()), "/diags/*");

    final Thread thread = new Thread(this::doStart);
    try {
      thread.start();
    } catch (Exception e) {
      LOGGER.error("Failed to start thread for application.", e);
      System.exit(1);
      throw e;
    }

    // Start the webserver.
    try {
      adminServer.start();
      LOGGER.info("Started admin service on port: {}.", serviceAdminPort);

      serviceState = State.STARTED;
      LOGGER.info("Service - {} is started.", getServiceName());

      thread.join();
      adminServer.join();
    } catch (Exception e) {
      LOGGER.error("Failed to start service servlet.");
    }
  }

  /**
   * @return service admin port.
   */
  private int getServiceAdminPort() {
    try {
      return appConfig.getInt("service.admin.port");
    } catch (Exception e) {
      try {
        ServerSocket socket = new ServerSocket(0);
        final int localPort = socket.getLocalPort();
        socket.close();
        return localPort;
      } catch (Exception e1) {
        throw new RuntimeException("Failed to allocate a port for service.");
      }
    }
  }

  public void shutdown() {
    try {
      adminServer.stop();
    } catch (Exception ex) {
      LOGGER.error("Error stopping admin server");
    }
    if (serviceState != State.STARTED) {
      LOGGER.info(
          "Service - {} is at state: {}. Expecting state: STARTED. Skipping shutdown...",
          getServiceName(), serviceState);
      return;
    }
    LOGGER.info("Trying to shutdown service - {}...", getServiceName());
    serviceState = State.STOPPING;
    doStop();
    serviceState = State.STOPPED;
    LOGGER.info("Stopping metrics registry");
    PlatformMetricsRegistry.stop();
    LOGGER.info("Service - {} is shutdown.", getServiceName());
  }

  enum State {
    NOT_STARTED,
    INITIALIZING,
    INITIALIZED,
    STARTING,
    STARTED,
    STOPPING,
    STOPPED
  }
}

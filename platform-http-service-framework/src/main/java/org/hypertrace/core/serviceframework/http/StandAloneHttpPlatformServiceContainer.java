package org.hypertrace.core.serviceframework.http;

import static io.grpc.Deadline.after;
import static java.util.concurrent.TimeUnit.SECONDS;

import io.micrometer.core.instrument.binder.grpc.MetricCollectingClientInterceptor;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hypertrace.core.grpcutils.client.GrpcChannelRegistry;
import org.hypertrace.core.grpcutils.client.GrpcRegistryConfig;
import org.hypertrace.core.serviceframework.PlatformService;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.core.serviceframework.http.jetty.JettyHttpServerBuilder;
import org.hypertrace.core.serviceframework.metrics.PlatformMetricsRegistry;

@Slf4j
public abstract class StandAloneHttpPlatformServiceContainer extends PlatformService {
  private HttpContainer container;
  private final GrpcChannelRegistry grpcChannelRegistry;

  public StandAloneHttpPlatformServiceContainer(ConfigClient config) {
    super(config);
    grpcChannelRegistry =
        new GrpcChannelRegistry(
            GrpcRegistryConfig.builder()
                .defaultInterceptor(
                    new MetricCollectingClientInterceptor(
                        PlatformMetricsRegistry.getMeterRegistry()))
                .build());
  }

  protected abstract List<HttpHandlerFactory> getHandlerFactories();

  @Override
  protected void doInit() {
    this.container =
        new JettyHttpServerBuilder().addHandlers(this.buildHandlerDefinitions()).build();
  }

  @Override
  protected void doStart() {
    log.info("Starting service {}", this.getServiceName());
    this.container.start();
    this.container.blockUntilStopped();
  }

  @Override
  protected void doStop() {
    log.info("Stopping service {}", this.getServiceName());
    grpcChannelRegistry.shutdown(after(10, SECONDS));
    this.container.stop();
  }

  @Override
  public boolean healthCheck() {
    return true;
  }

  private List<HttpHandlerDefinition> buildHandlerDefinitions() {
    HttpContainerEnvironment environment =
        new StandAloneHttpContainerEnvironment(
            this.grpcChannelRegistry, this.getLifecycle(), this.configClient);
    return this.getHandlerFactories().stream()
        .flatMap(handlerFactory -> handlerFactory.buildHandlers(environment).stream())
        .collect(Collectors.toUnmodifiableList());
  }
}

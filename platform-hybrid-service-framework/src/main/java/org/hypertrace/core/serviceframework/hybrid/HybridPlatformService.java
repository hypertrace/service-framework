package org.hypertrace.core.serviceframework.hybrid;

import com.google.common.collect.Streams;
import io.grpc.protobuf.services.HealthStatusManager;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hypertrace.core.grpcutils.client.InProcessGrpcChannelRegistry;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.core.serviceframework.grpc.GrpcPlatformServerDefinition;
import org.hypertrace.core.serviceframework.grpc.StandAloneGrpcPlatformServiceContainer;
import org.hypertrace.core.serviceframework.http.HttpContainer;
import org.hypertrace.core.serviceframework.http.HttpHandlerDefinition;
import org.hypertrace.core.serviceframework.http.HttpHandlerFactory;
import org.hypertrace.core.serviceframework.http.jetty.JettyHttpServerBuilder;

@Slf4j
public abstract class HybridPlatformService extends StandAloneGrpcPlatformServiceContainer {

  private HttpContainer httpContainer;

  public HybridPlatformService(ConfigClient configClient) {
    super(configClient);
  }

  @Override
  protected void doStart() {
    this.httpContainer.start();
    super.doStart();
  }

  @Override
  protected void doStop() {
    this.httpContainer.stop();
    super.doStop();
  }

  protected abstract List<GrpcPlatformServerDefinition> getServerDefinitions();

  protected List<HttpHandlerFactory> getHttpHandlerFactories() {
    return List.of();
  }

  protected List<HybridHttpHandlerFactory> getHybridHttpHandlerFactories() {
    return List.of();
  }

  @Override
  protected HybridServiceContainerEnvironment buildContainerEnvironment(
      InProcessGrpcChannelRegistry channelRegistry, HealthStatusManager healthStatusManager) {
    HybridServiceContainerEnvironment containerEnvironment =
        new StandAloneHybridServiceContainerEnvironment(
            channelRegistry,
            healthStatusManager,
            this.configClient,
            this.getInProcessServerName(),
            this.getLifecycle());
    this.httpContainer = this.buildHttpContainer(containerEnvironment);
    return containerEnvironment;
  }

  private HttpContainer buildHttpContainer(HybridServiceContainerEnvironment environment) {
    return new JettyHttpServerBuilder()
        .addHandlers(this.buildHandlerDefinitions(environment))
        .build();
  }

  private List<HttpHandlerDefinition> buildHandlerDefinitions(
      HybridServiceContainerEnvironment environment) {
    return Streams.concat(
            this.getHttpHandlerFactories().stream()
                .map(handlerFactory -> handlerFactory.buildHandlers(environment)),
            this.getHybridHttpHandlerFactories().stream()
                .map(handlerFactory -> handlerFactory.buildHandlers(environment)))
        .flatMap(Collection::stream)
        .collect(Collectors.toUnmodifiableList());
  }
}

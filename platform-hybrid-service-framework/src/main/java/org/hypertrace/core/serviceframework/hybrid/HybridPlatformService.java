package org.hypertrace.core.serviceframework.hybrid;

import io.grpc.protobuf.services.HealthStatusManager;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hypertrace.core.grpcutils.client.InProcessGrpcChannelRegistry;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.core.serviceframework.grpc.GrpcPlatformServerDefinition;
import org.hypertrace.core.serviceframework.grpc.StandAloneGrpcPlatformServiceContainer;
import org.hypertrace.core.serviceframework.http.HttpContainer;
import org.hypertrace.core.serviceframework.http.HttpContainerEnvironment;
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

  protected abstract List<HttpHandlerFactory> getHandlerFactories();

  @Override
  protected HybridServiceContainerEnvironment buildContainerEnvironment(
      InProcessGrpcChannelRegistry channelRegistry, HealthStatusManager healthStatusManager) {
    HybridServiceContainerEnvironment containerEnvironment = new StandAloneHybridServiceContainerEnvironment(
        channelRegistry,
        healthStatusManager,
        this.configClient,
        this.getInProcessServerName(),
        this.getLifecycle());
    this.httpContainer = this.buildHttpContainer(containerEnvironment);
    return containerEnvironment;
  }

  private HttpContainer buildHttpContainer(HttpContainerEnvironment environment) {
    return new JettyHttpServerBuilder()
        .addHandlers(this.buildHandlerDefinitions(environment))
        .build();
  }

  private List<HttpHandlerDefinition> buildHandlerDefinitions(
      HttpContainerEnvironment environment) {
    return this.getHandlerFactories().stream()
        .flatMap(handlerFactory -> handlerFactory.buildHandlers(environment).stream())
        .collect(Collectors.toUnmodifiableList());
  }

}

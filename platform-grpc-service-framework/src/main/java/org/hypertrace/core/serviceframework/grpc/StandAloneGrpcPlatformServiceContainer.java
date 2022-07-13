package org.hypertrace.core.serviceframework.grpc;

import io.grpc.protobuf.services.HealthStatusManager;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hypertrace.core.grpcutils.client.InProcessGrpcChannelRegistry;
import org.hypertrace.core.serviceframework.config.ConfigClient;

@Slf4j
public abstract class StandAloneGrpcPlatformServiceContainer extends GrpcPlatformServiceContainer {
  protected static final String DEFAULT_PORT_PATH = "service.port";

  public StandAloneGrpcPlatformServiceContainer(ConfigClient configClient) {
    super(configClient);
  }

  /**
   * @deprecated implement {@link #getServerDefinitions()} instead
   */
  @Deprecated
  protected GrpcPlatformServiceFactory getServiceFactory() {
    throw new UnsupportedOperationException(
        "getServiceFactory not implemented. Implement and use getServerDefinitions instead");
  }

  protected int getServicePort() {
    return this.getAppConfig().getInt(DEFAULT_PORT_PATH);
  }

  protected List<GrpcPlatformServerDefinition> getServerDefinitions() {
    return List.of(
        new GrpcPlatformServerDefinition(
            "networked-" + this.getServiceName(),
            this.getServicePort(),
            Set.of(this.getServiceFactory())));
  }

  @Override
  protected GrpcServiceContainerEnvironment buildContainerEnvironment(
      InProcessGrpcChannelRegistry channelRegistry, HealthStatusManager healthStatusManager) {
    return new StandAloneGrpcServiceContainerEnvironment(
        channelRegistry, healthStatusManager, this.configClient, this.getInProcessServerName());
  }
}

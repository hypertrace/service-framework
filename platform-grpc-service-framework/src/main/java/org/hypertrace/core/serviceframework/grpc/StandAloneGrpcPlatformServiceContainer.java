package org.hypertrace.core.serviceframework.grpc;

import io.grpc.protobuf.services.HealthStatusManager;
import java.util.Collection;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hypertrace.core.grpcutils.client.GrpcChannelRegistry;
import org.hypertrace.core.serviceframework.config.ConfigClient;

@Slf4j
public abstract class StandAloneGrpcPlatformServiceContainer extends GrpcPlatformServiceContainer {
  public StandAloneGrpcPlatformServiceContainer(ConfigClient configClient) {
    super(configClient);
  }

  protected abstract GrpcPlatformServiceFactory getServiceFactory();

  @Override
  protected final Collection<GrpcPlatformServiceFactory> getServiceFactories() {
    return Set.of(this.getServiceFactory());
  }

  @Override
  protected GrpcServiceContainerEnvironment buildContainerEnvironment(
      GrpcChannelRegistry channelRegistry, HealthStatusManager healthStatusManager) {
    return new StandAloneGrpcServiceContainerEnvironment(
        channelRegistry, healthStatusManager, this.configClient);
  }
}

package org.hypertrace.core.serviceframework.grpc;

import com.typesafe.config.Config;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.protobuf.services.HealthStatusManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hypertrace.core.grpcutils.client.InProcessGrpcChannelRegistry;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.core.serviceframework.spi.PlatformServiceLifecycle;

@AllArgsConstructor
class StandAloneGrpcServiceContainerEnvironment implements GrpcServiceContainerEnvironment {

  @Getter private final InProcessGrpcChannelRegistry channelRegistry;
  private final HealthStatusManager healthStatusManager;

  private final ConfigClient configClient;

  @Getter private final String inProcessChannelName;

  @Getter private final PlatformServiceLifecycle lifecycle;

  @Override
  public void reportServiceStatus(String serviceName, ServingStatus status) {
    this.healthStatusManager.setStatus(serviceName, status);
  }

  @Override
  public Config getConfig(String serviceName) {
    return this.configClient.getConfig(serviceName, null, null, null);
  }

  @Override
  public String getServiceName(String defaultServiceName) {
    String definedServiceName = this.configClient.getConfig().getString("service.name");
    return definedServiceName != null ? definedServiceName : defaultServiceName;
  }
}

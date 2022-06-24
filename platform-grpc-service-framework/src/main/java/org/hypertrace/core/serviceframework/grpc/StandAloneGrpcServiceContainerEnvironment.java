package org.hypertrace.core.serviceframework.grpc;

import com.typesafe.config.Config;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.protobuf.services.HealthStatusManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hypertrace.core.grpcutils.client.GrpcChannelRegistry;
import org.hypertrace.core.serviceframework.config.ConfigClient;

@AllArgsConstructor
public class StandAloneGrpcServiceContainerEnvironment implements GrpcServiceContainerEnvironment {

  @Getter private final GrpcChannelRegistry channelRegistry;
  private final HealthStatusManager healthStatusManager;

  private final ConfigClient configClient;

  @Override
  public void reportServiceStatus(String serviceName, ServingStatus status) {
    this.healthStatusManager.setStatus(serviceName, status);
  }

  @Override
  public Config getConfig(String serviceName) {
    return this.configClient.getConfig(serviceName, null, null, null);
  }
}

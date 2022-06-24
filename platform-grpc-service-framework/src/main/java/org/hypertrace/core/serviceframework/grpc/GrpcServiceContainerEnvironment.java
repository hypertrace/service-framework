package org.hypertrace.core.serviceframework.grpc;

import com.typesafe.config.Config;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import org.hypertrace.core.grpcutils.client.GrpcChannelRegistry;

public interface GrpcServiceContainerEnvironment {
  GrpcChannelRegistry getChannelRegistry();

  void reportServiceStatus(String serviceName, ServingStatus status);

  Config getConfig(String serviceName);
}

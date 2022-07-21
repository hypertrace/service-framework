package org.hypertrace.core.serviceframework.grpc;

import com.typesafe.config.Config;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import org.hypertrace.core.grpcutils.client.InProcessGrpcChannelRegistry;
import org.hypertrace.core.serviceframework.spi.PlatformServiceLifecycle;

public interface GrpcServiceContainerEnvironment {
  InProcessGrpcChannelRegistry getChannelRegistry();

  void reportServiceStatus(String serviceName, ServingStatus status);

  Config getConfig(String serviceName);

  String getInProcessChannelName();

  PlatformServiceLifecycle getLifecycle();

  String getServiceName(String defaultServiceName);
}

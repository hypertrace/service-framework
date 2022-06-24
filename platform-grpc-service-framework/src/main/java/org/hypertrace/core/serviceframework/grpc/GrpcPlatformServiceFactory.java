package org.hypertrace.core.serviceframework.grpc;

import java.util.List;

public interface GrpcPlatformServiceFactory {
  List<GrpcPlatformService> buildServices(GrpcServiceContainerEnvironment containerEnvironment);
}

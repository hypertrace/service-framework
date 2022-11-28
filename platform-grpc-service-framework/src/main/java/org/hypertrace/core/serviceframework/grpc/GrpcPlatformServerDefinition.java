package org.hypertrace.core.serviceframework.grpc;

import io.grpc.ServerInterceptor;
import java.util.Collection;
import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class GrpcPlatformServerDefinition {
  String name;
  int port;
  int maxInboundMessageSize;
  @Singular Collection<GrpcPlatformServiceFactory> serviceFactories;
  @Singular List<ServerInterceptor> serverInterceptors;
}

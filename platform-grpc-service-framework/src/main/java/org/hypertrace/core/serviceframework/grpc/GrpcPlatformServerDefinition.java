package org.hypertrace.core.serviceframework.grpc;

import io.grpc.ServerInterceptor;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class GrpcPlatformServerDefinition {
  String name;
  int port;
  int maxInboundMessageSize;
  @Builder.Default int maxRstPerMinute = 500;
  @Singular Collection<GrpcPlatformServiceFactory> serviceFactories;
  @Singular List<ServerInterceptor> serverInterceptors;
  Executor executor;
}

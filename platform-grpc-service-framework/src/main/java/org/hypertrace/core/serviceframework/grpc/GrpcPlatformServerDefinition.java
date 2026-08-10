package org.hypertrace.core.serviceframework.grpc;

import io.grpc.ServerInterceptor;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
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
  @Builder.Default Duration maxConnectionAge = Duration.ZERO;
  @Builder.Default Duration maxConnectionAgeGrace = Duration.ZERO;
  @Singular Collection<GrpcPlatformServiceFactory> serviceFactories;
  @Singular List<ServerInterceptor> serverInterceptors;

  /**
   * Optional executor used by the gRPC server to run RPC handler callbacks. When {@code null} (the
   * default), gRPC uses its built-in shared executor (an unbounded cached thread pool). Services
   * that block inside their handlers can supply a bounded or virtual-thread executor here to
   * control how handler work is dispatched.
   */
  @Nullable Executor executor;
}

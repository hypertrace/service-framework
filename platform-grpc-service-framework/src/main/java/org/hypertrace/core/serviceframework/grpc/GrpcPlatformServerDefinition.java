package org.hypertrace.core.serviceframework.grpc;

import java.util.Collection;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class GrpcPlatformServerDefinition {
  String name;
  int port;
  @Singular
  Collection<GrpcPlatformServiceFactory> serviceFactories;
}

package org.hypertrace.core.serviceframework.grpc;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import lombok.Value;

@Value
public class GrpcPlatformService {
  ServerServiceDefinition grpcServiceDefinition;

  public GrpcPlatformService(BindableService grpcService) {
    this(grpcService.bindService());
  }

  public GrpcPlatformService(ServerServiceDefinition grpcService) {
    this.grpcServiceDefinition = grpcService;
  }
}

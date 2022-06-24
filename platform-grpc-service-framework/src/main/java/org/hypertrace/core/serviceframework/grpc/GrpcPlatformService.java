package org.hypertrace.core.serviceframework.grpc;

import io.grpc.BindableService;
import lombok.Value;

@Value
public class GrpcPlatformService {
  BindableService grpcService;
}

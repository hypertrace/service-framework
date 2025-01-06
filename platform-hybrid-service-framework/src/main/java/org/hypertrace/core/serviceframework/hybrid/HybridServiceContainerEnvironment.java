package org.hypertrace.core.serviceframework.hybrid;

import org.hypertrace.core.serviceframework.grpc.GrpcServiceContainerEnvironment;
import org.hypertrace.core.serviceframework.http.HttpContainerEnvironment;

public interface HybridServiceContainerEnvironment
    extends GrpcServiceContainerEnvironment, HttpContainerEnvironment {

}

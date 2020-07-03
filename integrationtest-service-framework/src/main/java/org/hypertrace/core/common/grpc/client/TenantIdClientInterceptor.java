package org.hypertrace.core.common.grpc.client;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/**
 * Implementation of {@link ClientInterceptor} which sets the tenantId in RequestContext
 * in every request. Only used for testing because in real setup, envoy proxy does this logic.
 */
public class TenantIdClientInterceptor implements ClientInterceptor {
  public static final String TENANT_ID_HEADER_KEY = "x-tenant-id";

  public static final Metadata.Key<String> TENANT_ID_METADATA_KEY =
      Metadata.Key.of(TENANT_ID_HEADER_KEY, ASCII_STRING_MARSHALLER);

  private final String tenantId;

  public TenantIdClientInterceptor(String tenantId) {
    this.tenantId = tenantId;
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
      CallOptions callOptions, Channel next) {
    return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {

      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        headers.put(TENANT_ID_METADATA_KEY, tenantId);

        super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(
            responseListener) {
          @Override
          public void onHeaders(Metadata headers) {
            super.onHeaders(headers);
          }
        }, headers);
      }
    };
  }
}

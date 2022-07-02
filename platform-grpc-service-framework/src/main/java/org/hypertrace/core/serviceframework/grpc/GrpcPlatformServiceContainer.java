package org.hypertrace.core.serviceframework.grpc;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import io.grpc.Deadline;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.health.v1.HealthGrpc.HealthBlockingStub;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.protobuf.services.HealthStatusManager;
import java.io.IOException;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.hypertrace.core.grpcutils.client.InProcessGrpcChannelRegistry;
import org.hypertrace.core.grpcutils.server.InterceptorUtil;
import org.hypertrace.core.grpcutils.server.ServerManagementUtil;
import org.hypertrace.core.serviceframework.PlatformService;
import org.hypertrace.core.serviceframework.config.ConfigClient;

@Slf4j
abstract class GrpcPlatformServiceContainer extends PlatformService {
  private static final String DEFAULT_PORT_PATH = "service.port";
  private Server networkedServer;
  private Server inProcessServer;

  private final HealthStatusManager healthStatusManager = new HealthStatusManager();
  private InProcessGrpcChannelRegistry grpcChannelRegistry;
  private HealthBlockingStub healthClient;

  public GrpcPlatformServiceContainer(ConfigClient configClient) {
    super(configClient);
  }

  @Override
  protected void doInit() {
    this.grpcChannelRegistry = this.buildChannelRegistry();
    final ServerBuilder<?> networkedServerBuilder = ServerBuilder.forPort(this.getServicePort());
    final ServerBuilder<?> inProcessServerBuilder =
        InProcessServerBuilder.forName(this.getInProcessServerName());
    final GrpcServiceContainerEnvironment serviceContainerEnvironment =
        this.buildContainerEnvironment(this.grpcChannelRegistry, this.healthStatusManager);
    this.getServiceFactories().stream()
        .map(factory -> factory.buildServices(serviceContainerEnvironment))
        .flatMap(Collection::stream)
        .map(GrpcPlatformService::getGrpcService)
        .map(InterceptorUtil::wrapInterceptors)
        .forEach(
            service -> {
              networkedServerBuilder.addService(service);
              inProcessServerBuilder.addService(service);
            });
    inProcessServerBuilder.addService(this.healthStatusManager.getHealthService());
    networkedServerBuilder.addService(this.healthStatusManager.getHealthService());
    this.networkedServer = networkedServerBuilder.build();
    this.inProcessServer = inProcessServerBuilder.build();
    this.healthClient =
        HealthGrpc.newBlockingStub(this.grpcChannelRegistry.forName(this.getInProcessServerName()));
  }

  @Override
  protected void doStart() {
    log.info("Starting: {}", getServiceName());
    try {
      this.inProcessServer.start();
      this.networkedServer.start();
      this.networkedServer.awaitTermination();
      this.inProcessServer.awaitTermination();
    } catch (IOException e) {
      log.error("Fail to start the server.");
      throw new RuntimeException(e);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(ie);
    }
  }

  @Override
  protected void doStop() {
    healthStatusManager.enterTerminalState();
    grpcChannelRegistry.shutdown(Deadline.after(10, SECONDS));
    ServerManagementUtil.shutdownServer(
        this.networkedServer, "networked:" + this.getServiceName(), Deadline.after(1, MINUTES));
    ServerManagementUtil.shutdownServer(
        this.inProcessServer, "inprocess:" + this.getServiceName(), Deadline.after(1, MINUTES));
  }

  @Override
  public boolean healthCheck() {
    try {
      return this.healthClient
          .withDeadlineAfter(10, SECONDS)
          .check(HealthCheckRequest.getDefaultInstance())
          .getStatus()
          .equals(ServingStatus.SERVING);
    } catch (Exception e) {
      log.debug("health check error", e);
      return false;
    }
  }

  protected int getServicePort() {
    return this.getAppConfig().getInt(DEFAULT_PORT_PATH);
  }

  protected InProcessGrpcChannelRegistry buildChannelRegistry() {
    return new InProcessGrpcChannelRegistry();
  }

  protected String getInProcessServerName() {
    return this.getServiceName();
  }

  protected abstract Collection<GrpcPlatformServiceFactory> getServiceFactories();

  protected abstract GrpcServiceContainerEnvironment buildContainerEnvironment(
      InProcessGrpcChannelRegistry channelRegistry, HealthStatusManager healthStatusManager);
}

package org.hypertrace.core.serviceframework.grpc;

import static io.grpc.Deadline.after;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.hypertrace.core.grpcutils.client.InProcessGrpcChannelRegistry;
import org.hypertrace.core.grpcutils.server.InterceptorUtil;
import org.hypertrace.core.grpcutils.server.ServerManagementUtil;
import org.hypertrace.core.serviceframework.PlatformService;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.core.serviceframework.spi.PlatformServiceLifecycle.State;

@Slf4j
abstract class GrpcPlatformServiceContainer extends PlatformService {

  private List<ConstructedServer> servers;
  private final List<PlatformPeriodicTaskDefinition> taskDefinitions = new LinkedList<>();
  private final List<ScheduledFuture<?>> scheduledFutures = new LinkedList<>();
  private ScheduledExecutorService periodicTaskExecutor;

  private final HealthStatusManager healthStatusManager = new HealthStatusManager();
  private InProcessGrpcChannelRegistry grpcChannelRegistry;
  private HealthBlockingStub healthClient;

  public GrpcPlatformServiceContainer(ConfigClient configClient) {
    super(configClient);
  }

  @Override
  protected void doInit() {
    this.grpcChannelRegistry = this.buildChannelRegistry();
    Map<GrpcPlatformServerDefinition, ServerBuilder<?>> serverBuilderMap =
        this.getServerDefinitions().stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    Function.identity(),
                    definition -> ServerBuilder.forPort(definition.getPort())));
    final ServerBuilder<?> inProcessServerBuilder =
        InProcessServerBuilder.forName(this.getInProcessServerName())
            .addService(this.healthStatusManager.getHealthService());
    final GrpcServiceContainerEnvironment serviceContainerEnvironment =
        this.buildContainerEnvironment(this.grpcChannelRegistry, this.healthStatusManager);
    List<ConstructedServer> providedServers =
        serverBuilderMap.entrySet().stream()
            .map(
                entry ->
                    this.constructServer(
                        entry.getKey(),
                        entry.getValue(),
                        inProcessServerBuilder,
                        serviceContainerEnvironment))
            .collect(Collectors.toUnmodifiableList());

    this.servers =
        Stream.concat(
                Stream.of(
                    new ConstructedServer(
                        this.getInProcessServerName(), inProcessServerBuilder.build())),
                providedServers.stream())
            .collect(Collectors.toUnmodifiableList());

    this.healthClient =
        HealthGrpc.newBlockingStub(this.grpcChannelRegistry.forName(this.getInProcessServerName()));
  }

  private ConstructedServer constructServer(
      GrpcPlatformServerDefinition serverDefinition,
      ServerBuilder<?> networkedBuilder,
      ServerBuilder<?> inProcessServerBuilder,
      GrpcServiceContainerEnvironment containerEnvironment) {
    log.info(
        "Building server [{}] on port [{}]",
        serverDefinition.getName(),
        serverDefinition.getPort());
    serverDefinition.getServiceFactories().stream()
        .map(factory -> factory.buildServices(containerEnvironment))
        .flatMap(Collection::stream)
        .map(GrpcPlatformService::getGrpcService)
        .map(InterceptorUtil::wrapInterceptors)
        .forEach(
            service -> {
              log.info(
                  "Adding service [{}] to server [{}]",
                  service.getServiceDescriptor().getName(),
                  serverDefinition.getName());
              networkedBuilder.addService(service);
              log.info(
                  "Adding service [{}] to in-process server [{}]",
                  service.getServiceDescriptor().getName(),
                  getInProcessServerName());
              inProcessServerBuilder.addService(service);
            });

    return new ConstructedServer(serverDefinition.getName(), networkedBuilder.build());
  }

  @Override
  protected void doStart() {
    log.info("Starting all services: [{}]", getServiceName());
    this.startManagedPeriodicTasks();
    this.servers.forEach(this::startServer);
    this.servers.forEach(this::awaitServerTermination);
  }

  private void startManagedPeriodicTasks() {
    this.periodicTaskExecutor = this.buildTaskExecutor(this.taskDefinitions.size());
    this.taskDefinitions.forEach(this::startManagedPeriodicTask);
    this.taskDefinitions.clear();
  }

  private void startManagedPeriodicTask(PlatformPeriodicTaskDefinition taskDefinition) {
    log.info(
        "Starting managed periodic task [{}] with an initial delay of {} and period of {}",
        taskDefinition.getName(),
        taskDefinition.getInitialDelay(),
        taskDefinition.getPeriod());
    this.scheduledFutures.add(
        this.periodicTaskExecutor.scheduleAtFixedRate(
            taskDefinition.getRunnable(),
            taskDefinition.getInitialDelay().toMillis(),
            taskDefinition.getPeriod().toMillis(),
            MILLISECONDS));
  }

  private void startServer(ConstructedServer constructedServer) {
    try {
      log.info("Starting server [{}]", constructedServer.getName());
      constructedServer.getServer().start();
    } catch (IOException e) {
      log.error("Fail to start the server.");
      throw new RuntimeException(e);
    }
  }

  private void awaitServerTermination(ConstructedServer constructedServer) {
    try {
      constructedServer.getServer().awaitTermination();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void doStop() {
    this.scheduledFutures.forEach(future -> future.cancel(true));
    healthStatusManager.enterTerminalState();
    grpcChannelRegistry.shutdown(after(10, SECONDS));
    this.servers.forEach(
        constructedServer ->
            ServerManagementUtil.shutdownServer(
                constructedServer.getServer(), constructedServer.getName(), after(30, SECONDS)));
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
      log.error("Health check error", e);
      return false;
    }
  }

  protected InProcessGrpcChannelRegistry buildChannelRegistry() {
    return new InProcessGrpcChannelRegistry();
  }

  protected String getInProcessServerName() {
    return this.getServiceName();
  }

  protected ScheduledExecutorService buildTaskExecutor(int taskCount) {
    // Between 1-4 threads
    return Executors.newScheduledThreadPool(Math.max(1, Math.min(taskCount, 4)));
  }

  protected void registerManagedPeriodicTask(PlatformPeriodicTaskDefinition periodicTask) {
    if (State.STARTED.compareTo(this.getServiceState()) > 0) {
      // Not yet started, just queue it
      this.taskDefinitions.add(periodicTask);
    } else if (State.STARTED.equals(this.getServiceState())) {
      // Already started so just start it immediately instead of queueing it
      this.startManagedPeriodicTask(periodicTask);
    } else {
      throw new UnsupportedOperationException(
          "Cannot register period task for server at state: " + this.getServiceState().name());
    }
  }

  protected abstract List<GrpcPlatformServerDefinition> getServerDefinitions();

  protected abstract GrpcServiceContainerEnvironment buildContainerEnvironment(
      InProcessGrpcChannelRegistry channelRegistry, HealthStatusManager healthStatusManager);

  @Value
  private static class ConstructedServer {
    String name;
    Server server;
  }
}

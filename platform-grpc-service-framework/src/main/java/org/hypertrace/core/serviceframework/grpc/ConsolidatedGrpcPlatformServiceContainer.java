package org.hypertrace.core.serviceframework.grpc;

import io.grpc.protobuf.services.HealthStatusManager;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hypertrace.core.grpcutils.client.GrpcChannelRegistry;
import org.hypertrace.core.grpcutils.client.InProcessGrpcChannelRegistry;
import org.hypertrace.core.serviceframework.config.ConfigClient;

@Slf4j
public abstract class ConsolidatedGrpcPlatformServiceContainer
    extends GrpcPlatformServiceContainer {
  private static final String AUTHORITY_OVERRIDE_PATH = "service.authorities";

  public ConsolidatedGrpcPlatformServiceContainer(ConfigClient configClient) {
    super(configClient);
  }

  @Override
  protected InProcessGrpcChannelRegistry buildChannelRegistry() {
    return new InProcessGrpcChannelRegistry(
        this.getAuthorityInProcessOverrideMap(this.getInProcessServerName()));
  }

  @Override
  protected GrpcServiceContainerEnvironment buildContainerEnvironment(
      GrpcChannelRegistry channelRegistry, HealthStatusManager healthStatusManager) {
    return new ConsolidatedGrpcServiceContainerEnvironment(channelRegistry, healthStatusManager);
  }

  protected Collection<String> getAuthoritiesToTreatAsInProcess() {
    if (this.getAppConfig().hasPath(AUTHORITY_OVERRIDE_PATH)) {
      return this.getAppConfig().getStringList(AUTHORITY_OVERRIDE_PATH);
    }
    return Collections.emptySet();
  }

  private Map<String, String> getAuthorityInProcessOverrideMap(String inProcessName) {
    return this.getAuthoritiesToTreatAsInProcess().stream()
        .collect(Collectors.toUnmodifiableMap(Function.identity(), unused -> inProcessName));
  }
}

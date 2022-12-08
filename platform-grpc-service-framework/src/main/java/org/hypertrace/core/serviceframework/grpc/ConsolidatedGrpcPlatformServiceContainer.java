package org.hypertrace.core.serviceframework.grpc;

import io.grpc.protobuf.services.HealthStatusManager;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hypertrace.core.grpcutils.client.InProcessGrpcChannelRegistry;
import org.hypertrace.core.serviceframework.config.ConfigClient;

@Slf4j
public abstract class ConsolidatedGrpcPlatformServiceContainer
    extends GrpcPlatformServiceContainer {
  private static final String AUTHORITY_OVERRIDE_PATH = "service.authorities";
  private static final String DEFAULT_PORT_PATH = "service.port";

  public ConsolidatedGrpcPlatformServiceContainer(ConfigClient configClient) {
    super(configClient);
  }

  @Override
  protected GrpcServiceContainerEnvironment buildContainerEnvironment(
      InProcessGrpcChannelRegistry channelRegistry, HealthStatusManager healthStatusManager) {
    return new ConsolidatedGrpcServiceContainerEnvironment(
        channelRegistry, healthStatusManager, this.getInProcessServerName(), this.getLifecycle());
  }

  @Override
  protected List<GrpcPlatformServerDefinition> getServerDefinitions() {
    return List.of(
        GrpcPlatformServerDefinition.builder()
            .name("networked-" + this.getServiceName())
            .port(this.getServicePort())
            .serviceFactories(this.getServiceFactories())
            .build());
  }

  /**
   * @deprecated - implement {@link #getServerDefinitions()}} instead
   */
  @Deprecated
  protected Collection<GrpcPlatformServiceFactory> getServiceFactories() {
    return Collections.emptySet();
  }

  protected int getServicePort() {
    return this.getAppConfig().getInt(DEFAULT_PORT_PATH);
  }

  protected Collection<String> getAuthoritiesToTreatAsInProcess() {
    if (this.getAppConfig().hasPath(AUTHORITY_OVERRIDE_PATH)) {
      return this.getAppConfig().getStringList(AUTHORITY_OVERRIDE_PATH);
    }
    return Collections.emptySet();
  }

  protected Map<String, String> getAuthorityInProcessOverrideMap() {
    return this.getAuthoritiesToTreatAsInProcess().stream()
        .collect(
            Collectors.toUnmodifiableMap(
                Function.identity(), unused -> this.getInProcessServerName()));
  }
}

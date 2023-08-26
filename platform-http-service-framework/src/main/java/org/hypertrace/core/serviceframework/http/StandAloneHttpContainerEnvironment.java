package org.hypertrace.core.serviceframework.http;

import com.typesafe.config.Config;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hypertrace.core.grpcutils.client.InProcessGrpcChannelRegistry;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.core.serviceframework.spi.PlatformServiceLifecycle;

@AllArgsConstructor
public class StandAloneHttpContainerEnvironment implements HttpContainerEnvironment {
  @Getter private final InProcessGrpcChannelRegistry channelRegistry;
  @Getter private final PlatformServiceLifecycle lifecycle;
  private final ConfigClient configClient;

  @Override
  public Config getConfig(String serviceName) {
    return this.configClient.getConfig(serviceName, null, null, null);
  }
}

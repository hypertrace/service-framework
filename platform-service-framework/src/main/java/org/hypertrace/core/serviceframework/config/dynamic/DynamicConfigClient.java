package org.hypertrace.core.serviceframework.config.dynamic;

import org.hypertrace.core.serviceframework.config.ConfigClient;

/**
 * DynamicConfigClient is an abstract class provide the interfaces to subscribe to dynamic config
 * changes.
 */
public abstract class DynamicConfigClient implements ConfigClient {

  /**
   * @param configName the config name/prefix you wanna subscribe to.
   * @param callback the callback registered to the given configs change.
   */
  abstract void subscribeConfigChange(String configName, ConfigChangeCallback callback);
}

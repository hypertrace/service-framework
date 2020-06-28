package org.hypertrace.core.serviceframework.config.dynamic;

import com.typesafe.config.Config;

public interface ConfigChangeCallback {

  /**
   * @param config The new configs returned from remote.
   * @param exception Server side exception.
   */
  void onReturn(Config config, Exception exception);
}

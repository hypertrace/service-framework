package org.hypertrace.core.serviceframework.config;

import com.typesafe.config.Config;

public interface ConfigClient {

  /**
   * Fetch configs based on existing Environment Variables.
   *
   * @return Config for this container based on existing Environment Variables.
   */
  Config getConfig();

  /**
   * Fetch full application configs based on given parameters.
   *
   * @param service Service Name, not null.
   * @param cluster Cluster Name, not null.
   * @param pod Pod Name, a.k.a as an instance name, could be null for stateless services.
   * @param container Container Name, could be null if not co-located in same pod.
   */
  Config getConfig(String service, String cluster, String pod, String container);
}

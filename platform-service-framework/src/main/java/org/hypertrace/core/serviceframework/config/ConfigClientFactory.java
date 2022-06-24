package org.hypertrace.core.serviceframework.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ConfigClientFactory {

  private static Map<String, ConfigClient> configClientMap = new HashMap<>();

  private ConfigClientFactory() {}

  public static synchronized ConfigClient getClient() {
    String bootstrapUri = ConfigUtils.getEnvironmentProperty("bootstrap.config.uri");
    if (bootstrapUri != null) {
      return getClient(bootstrapUri);
    } else {
      URL url = ConfigClientFactory.class.getClassLoader().getResource("configs");
      if (url != null) {
        return getClient(url.toExternalForm());
      }
    }
    throw new RuntimeException(
        "Failed to init ConfigClient, Please specify environment property: bootstrap.config.uri");
  }

  public static synchronized ConfigClient getClient(String bootstrapUri) {
    if (!configClientMap.containsKey(bootstrapUri)) {
      try {
        URI uri = new URI(bootstrapUri);
        final String scheme = uri.getScheme();

        switch (scheme) {
          case "file":
            configClientMap.put(bootstrapUri, new DirectoryBasedConfigClient(uri.getPath()));
            break;
          default:
            throw new UnsupportedOperationException(
                "Not yet supported ConfigClient bootstrap URI scheme: " + scheme);
        }
      } catch (URISyntaxException e) {
        throw new RuntimeException(
            "Failed to init ConfigClient with a bad URI: " + bootstrapUri, e);
      }
    }
    return configClientMap.get(bootstrapUri);
  }

  public static ConfigClient getClientForService(String serviceName) {
    return getClient(
        ConfigClientFactory.class
            .getClassLoader()
            .getResource("configs/" + serviceName)
            .toExternalForm());
  }
}

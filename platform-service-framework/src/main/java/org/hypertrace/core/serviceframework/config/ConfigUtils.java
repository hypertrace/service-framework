package org.hypertrace.core.serviceframework.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValue;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigUtils {

  private static Logger LOGGER = LoggerFactory.getLogger(ConfigUtils.class);

  public static void logConfFile(Config configs) {
    for (Entry<String, ConfigValue> entry : configs.entrySet()) {
      LOGGER.info("{} = {}", entry.getKey(), entry.getValue());
    }
  }

  public static String getStringConfig(Config config, String path, String defaultVal) {
    if (config.hasPath(path)) {
      return config.getString(path);
    }
    return defaultVal;
  }

  public static boolean getBooleanConfig(Config config, String path, boolean defaultVal) {
    if (config.hasPath(path)) {
      return config.getBoolean(path);
    }
    return defaultVal;
  }

  public static int getIntConfig(Config config, String path, int defaultVal) {
    if (config.hasPath(path)) {
      return config.getInt(path);
    }
    return defaultVal;
  }

  public static long getLongConfig(Config config, String path, long defaultVal) {
    if (config.hasPath(path)) {
      return config.getLong(path);
    }
    return defaultVal;
  }

  public static List<String> getStringsConfig(Config config, String path, List<String> defaultVal) {
    if (config.hasPath(path)) {
      return config.getStringList(path);
    }
    return defaultVal;
  }

  /**
   * Utility method to fetch the configuration at a given path as simple java properties. Assumes
   * there is no nesting at the given path. All the values are converted to strings
   *
   * @param config
   * @param path
   * @return properties at the given path.
   * @throws com.typesafe.config.ConfigException.Missing if value is absent or null
   */
  public static Properties getPropertiesConfig(Config config, String path) {
    Properties properties = new Properties();
    Config subconfig = config.getConfig(path);
    subconfig.entrySet().stream()
        .forEach(
            entry -> properties.setProperty(entry.getKey(), subconfig.getString(entry.getKey())));
    return properties;
  }

  /**
   * Utility method to fetch the configuration at a given path as simple flat java map of key value
   * pairs. All the values are converted to strings. Assumes there is no nesting at the given path.
   *
   * @param config
   * @param path
   * @return map of key-value pairs at the given path
   * @throws com.typesafe.config.ConfigException.Missing if value is absent or null
   */
  public static Map<String, String> getFlatMapConfig(Config config, String path) {
    Map<String, String> propertiesMap = new HashMap<>();
    Config subconfig = config.getConfig(path);
    subconfig.entrySet().stream()
        .forEach(entry -> propertiesMap.put(entry.getKey(), subconfig.getString(entry.getKey())));
    return propertiesMap;
  }

  public static String getEnvironmentProperty(String key) {
    String value = System.getProperty(key);
    if (value == null || value.isEmpty()) {
      value = System.getenv(key.toUpperCase().replace(".", "_"));
    }
    if (value == null || value.isEmpty()) {
      LOGGER.warn("Cannot find Property: {} in neither JVM parameter nor OS environment", key);
      return null;
    }
    return value;
  }

  public static String propertiesAsList(Properties properties) {
    if (properties == null) return null;

    StringWriter writer = new StringWriter();
    PrintWriter pw = new PrintWriter(writer);
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      if (entry.getValue() instanceof Config) {
        pw.println(
            entry.getKey()
                + "="
                + ((Config) entry.getValue())
                    .root()
                    .render(
                        ConfigRenderOptions.concise()
                            .setJson(true)
                            .setComments(false)
                            .setFormatted(true)));
      } else {
        pw.println(entry.getKey() + "=" + entry.getValue());
      }
    }
    return writer.toString();
  }
}

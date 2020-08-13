package org.hypertrace.core.serviceframework.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigUtilsTest {

  @Test
  public void whenPropsIsNullExpectANullString() {
    assertNull(ConfigUtils.propertiesAsList(null));
  }

  @Test
  public void whenPropertiesContainConfigValue_thenExpectItToBeHandled() {
    Properties props = new Properties();
    props.put("key", "value");
    Map<String, String> map = new HashMap<>();
    map.put("k", "v");
    props.put("config.key", ConfigFactory.parseMap(map));

    String result = ConfigUtils.propertiesAsList(props);
    assertTrue(result.contains("k"));
    assertTrue(result.contains("key"));
  }

  @Test
  public void testConfigAsMap() {
    Properties properties = new Properties();
    properties.setProperty("root.parent1.child1", "value1");
    properties.setProperty("root.parent1.child2", "value2");
    properties.setProperty("root.parent2.child3", "value3");
    properties.setProperty("root.parent2.child4", "value4");
    properties.setProperty("root.child1", "value5");
    properties.setProperty("root.child2", "value6");

    Config config = ConfigFactory.parseProperties(properties);

    Map<String, String> subconfigMap = ConfigUtils.getFlatMapConfig(config, "root.parent1");
    assertEquals("value1", subconfigMap.get("child1"));
    assertEquals("value2", subconfigMap.get("child2"));
    assertEquals(2, subconfigMap.size());
  }

  @Test
  public void testConfigAsProperties() {
    Properties properties = new Properties();
    properties.setProperty("root.parent1.child1", "value1");
    properties.setProperty("root.parent1.child2", "value2");
    properties.setProperty("root.parent2.child3", "value3");
    properties.setProperty("root.parent2.child4", "value4");
    properties.setProperty("root.child1", "value5");
    properties.setProperty("root.child2", "value6");

    Config config = ConfigFactory.parseProperties(properties);

    Properties configProperties = ConfigUtils.getPropertiesConfig(config, "root.parent1");
    assertEquals("value1", configProperties.get("child1"));
    assertEquals("value2", configProperties.get("child2"));
    assertEquals(2, configProperties.size());
  }

  @Test
  public void testDefaultValues() {
    Config config = ConfigFactory.empty();
    assertEquals(1, ConfigUtils.getIntConfig(config, "test", 1));
    assertFalse(ConfigUtils.getBooleanConfig(config, "test", false));
    assertEquals("bar", ConfigUtils.getStringConfig(config, "test", "bar"));
    assertEquals(1, ConfigUtils.getLongConfig(config, "test", 1L));
    assertEquals(List.of("one"), ConfigUtils.getStringsConfig(config, "test", List.of("one")));
  }
}

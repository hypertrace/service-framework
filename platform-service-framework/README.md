# Platform Service Framework

Platform service framework provides unified logging/metrics/service registry functionalities to support Platform Service development and deployment.

You could write your own service logic by extending `org.hypertrace.core.serviceframework.PlatformService` and implementing doInit/doStart/doShutdown/healthCheck methods there.

The framework provides configs/logging/metrics support for now.

## Starter ##
In order to start the application, we need to explicitly set below java system properties:

`service.name`, `cluster.name`, `pod.name`, `bootstrap.config.uri`,

 or OS environment variables:

`SERVICE_NAME`, `CLUSTER_NAME`, `POD_NAME`, `BOOTSTRAP_CONFIG_URI`.

Below is a sample command to start application from java cmd:
```
/Library/Java/JavaVirtualMachines/jdk1.8.0_192.jdk/Contents/Home/bin/java \
-Dservice.name=sample-app \
-Dcluster.name=staging \
-Dpod.name=gcp01 \
-Dbootstrap.config.uri=file:///Users/user1/workspace/platform-service-framework-sample-app/out/production/resources/configs \
-classpath /Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/platform-service-framework-0.2.0-prerelease.106-SNAPSHOT.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/config-1.3.2.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/metrics-jvm-4.1.0.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/simpleclient_dropwizard-0.6.0.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/metrics-core-4.1.0.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/logback-kafka-appender-0.2.0-RC1.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/kafka-clients-2.1.0.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/slf4j-api-1.7.26.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/janino-3.0.6.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/reflections-0.9.9.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/simpleclient_servlet-0.6.0.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/jetty-servlet-8.1.7.v20120910.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/logback-core-1.2.3.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/commons-compiler-3.0.6.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/guava-15.0.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/javassist-3.18.2-GA.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/annotations-2.0.1.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/zstd-jni-1.3.5-4.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/lz4-java-1.5.0.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/snappy-java-1.1.7.2.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/simpleclient_common-0.6.0.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/simpleclient-0.6.0.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/jetty-security-8.1.7.v20120910.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/jetty-server-8.1.7.v20120910.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/javax.servlet-3.0.0.v201112011016.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/jetty-continuation-8.1.7.v20120910.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/jetty-http-8.1.7.v20120910.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/jetty-io-8.1.7.v20120910.jar:/Users/user1/workspace/platform-service-framework-sample-app/build/distributions/platform-service-framework-sample-app-0.2.0-prerelease.106-SNAPSHOT/lib/jetty-util-8.1.7.v20120910.jar \
org.hypertrace.core.serviceframework.PlatformServiceLauncher
```

Below is a sample command to start application from docker image:
```
docker run \
  -v /Users/user1/workspace/platform-service-framework-sample-app/src/main/resources/configs:/app/configs \
  -e BOOTSTRAP_CONFIG_URI='file:///app/configs' \
  -e SERVICE_NAME='sample-app' \
  -e CLUSTER_NAME='staging' \
  -e POD_NAME='gcp01' \
  my-services/platform-service-framework-sample-app:1.0.0
```

## Configuration ##
ConfigClient is the interface to get application configuration.

`ConfigClientFactory` is used to provide a ConfigClient. It takes jvm system property: `bootstrap.config.uri` or OS environment Variable: `BOOTSTRAP_CONFIG_URI` to bootstrap.

Currently, we only have one implementation: `DirectoryBasedConfigClient`, which take a filesystem based hierarchy directory as the input. E.g. `file:///Users/user1/workspace/platform-service-framework-sample-app/out/production/resources/configs`.


The default config file name is `application.conf`.

The configs will override based on the ordering from top to bottom. It's fine to skip files at any level.
e.g. The override convention for application.conf is:
 * /path/to/configs/application.conf
 * /path/to/configs/[service]/application.conf
 * /path/to/configs/[service]/[cluster]/application.conf
 * /path/to/configs/[service]/[cluster]/[pod]/application.conf
 * /path/to/configs/[service]/[cluster]/[pod]/[container]/application.conf


Please ensure to put configuration `main.class` to the implementation for `PlatformService`.
E.g.
```$xslt
main.class = org.hypertrace.core.example.SamplePlatformService
```


## Logging ##
For logging, framework keeps slf4j interfaces, so the code will remain the same. E.g.
```$xslt

  private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryBasedConfigClient.class);
  LOGGER.info("Some messages");
```
For logging, the service provides 3 implementations, STDOUT/FILE/KAFKA.
Please refer to `log4j2.properties` for more details of configuration.

## Metrics ##
For metrics, framework provides a wrapper on top of it for metrics register.
```$xslt
Counter someRandomCounter = new Counter();
PlatformMetricsRegistry.register("some.random.count", someRandomCounter);
```
PlatformMetricsRegistry also by default registered all the jvm/memory/cpu/thread metrics.

All the metrics are exposed as Prometheus metrics format through http servlet on `${serviceName}:${servicePort}/metrics`.

Below are sample configs for metrics reporter.

```$xslt

metrics.reporter.names = ["console", "prometheus"]
metrics.reporter.prefix = org.hypertrace.core.sample-app
metrics.reporter.console.reportInterval = 35

```

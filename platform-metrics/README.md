# Platform Metrics Library
Platform metrics library is a utility library to help with:
 * Automatically report JVM, GC, memory, threads, and other common metrics
 * Make it easy to collect custom metrics in the service code very easily
 * Report all the metrics to either monitoring backends like Prometheus or
   log them to files, so that we can have access to metrics in all environments

The metric backends, report interval, and the default tags for all the metrics
are configurable. Different reporters supported are:
 * prometheus
 * logging --> to log metrics into log files or console
 * testing --> in-memory reporter. Purely for unit tests.

The default metrics backend is "prometheus" with 30s reporting interval.

## Usage

### Initialization
```java
// HOCON config object with configuration.
Config config = ConfigFactory.parseMap(Map.of(
    "reporter.names", List.of("testing"),
    "reportInterval", "10",
    "defaultTags", List.of("region", "us-east-1")
));
PlatformMetricsRegistry.initMetricsRegistry("test-service", config);
```
This will automatically give a bunch of common metrics for the service.

### Custom metrics reporting

```java
// Timer usage
Timer timer = PlatformMetricsRegistry.registerTimer("my.timer", Map.of("foo", "bar"));
timer.record(1, TimeUnit.SECONDS);

// Counter usage
Counter counter = PlatformMetricsRegistry.registerCounter("my.counter", Map.of("foo", "bar"));
counter.increment();
```

## References
This library uses MicroMeter for metrics, and the API of MicroMeter is exposed directly
to leverage the power of it. See https://micrometer.io/docs for more details.

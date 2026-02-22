# Falchion

Falchion is an antifragile JVM container.
It manages a pool of JVM processes sharing the same port via SO_REUSEPORT, enabling zero-downtime restarts and automatic JVM parameter tuning.

Requires JDK 9+ (SO_REUSEPORT support). SO_REUSEPORT is available on Linux and macOS.

When the container starts, it forks JVM processes until the given pool size.
If the container catches a HUP signal, it creates new JVM processes and kills old processes gracefully.

## Modules

- **falchion-container** - The main container that manages JVM processes
- **falchion-jetty9-connector** - Jetty 9 `ServerConnector` with SO_REUSEPORT support
- **falchion-example-jetty9** - Example using embedded Jetty directly
- **falchion-example-spring-boot** - Example using Spring Boot with Jetty and Auto Tuning
- **falchion-loadtest** - Gatling load test for the Spring Boot example

## Usage

```
% java -jar falchion-container-0.3.0.jar [mainClass] -cp classpath
```

## Options

- `-cp CLASSPATH` Classpath
- `-basedir DIR` The base directory of jar files
- `-v VERSION` Application version
- `-p SIZE` The pool size of JVM processes. (default: 1)
- `--admin-port PORT` A port number of the API server. (default: 44010)
- `--lifetime SEC` The lifetime of one JVM process in seconds. If you set the option to 3600, the container will kill all old JVMs and create new JVMs gracefully every hour.
- `--java-opts JAVA_OPTS` JVM options for worker processes.
- `-m MONITORS` Adds monitors for monitoring a JVM process.
  Available monitors:
    - `GCUTIL_JSTAT` GC statistics using jstat
    - `METRICS_JMX` Metrics using JMX
- `--auto-tuning` Tuning JVM parameters automatically. See [Auto tuning](#auto-tuning) section.
- `--evaluator EVALUATOR` JVM parameter evaluator for auto tuning. Available values: `MIN_GC_TIME` (default), `MIN_FULL_GC_COUNT`.

When using the options `-basedir` and `-v`, Falchion container generates classpath using basedir and application version.
Please set the folder hierarchy like the example below.

```
applicationBaseDir/    <- basedir
  0.1.0/               <- application version
    applicationA.jar
    applicationB.jar
    subDir/
      applicationC.jar
  0.1.1/
```

## REST API

![api](http://i.imgur.com/iIRC5Ix.png)

Falchion container provides HTTP APIs for getting the information of JVM processes or refreshing JVM processes.
The API server listens on port 44010 by default (configurable with `--admin-port`).

### GET /jvms

```
% curl -i http://localhost:44010/jvms
HTTP/1.1 200 OK
Date: Mon, 02 May 2016 10:11:24 GMT
Content-type: application/json
Content-length: 28

[{"id":"5jiic","pid":21914}]
```

### GET /jvm/{id}

```
% curl -i http://localhost:44010/jvm/5jiic
HTTP/1.1 200 OK
Date: Mon, 02 May 2016 10:12:23 GMT
Content-type: application/json
Content-length: 182

{"id":"5jiic","pid":21914,"uptime":258357,"stats":[{"type":"gcutil","S0":0.0,"S1":100.0,"E":16.88,"O":3.1,"M":94.86,"CCS":80.13,"YGC":1,"YGCT":0.004,"FGC":0,"FGCT":0.0,"GCT":0.004}]}
```

### POST /container/refresh

Kill all old JVMs and create new JVMs gracefully.

### POST /container/refresh/{version}

Kill all old JVMs and create new JVMs gracefully.
Change classpath using new version.

## Auto tuning

Falchion can optimize JVM options automatically.
When auto tuning is enabled, the container samples JVM options (heap size, GC ratios, etc.) with variance around a base value, monitors each process's performance, and feeds back the results to converge on better parameters over time.

### How it works

1. On startup, `AutoOptimizableProcessSupplier` generates JVM options (`-Xms`, `-Xmx`, `-XX:NewRatio`, `-XX:SurvivorRatio`, etc.) by sampling around default values with some variance.
2. Each child process runs with these options while monitors (`GCUTIL_JSTAT`, `METRICS_JMX`) collect GC statistics and request metrics.
3. On refresh, the evaluator scores all running processes and selects the best one. The next generation of processes is sampled around the best process's options.

### Example

```
% falchion your.app.MainClass \
    --auto-tuning \
    -m GCUTIL_JSTAT METRICS_JMX \
    -p 2 \
    --lifetime 3600 \
    -cp /path/to/your/app.jar
```

- `--auto-tuning` is required to enable the feature.
- `-m GCUTIL_JSTAT METRICS_JMX` is required because the evaluator needs both `GcStat` and `MetricsStat` to calculate scores. Without monitors, all scores become -1 and tuning will not function.
- `-p 2` or higher is recommended so that multiple processes can be compared.
- `--lifetime 3600` triggers automatic refresh every hour, which runs the feedback loop.

### Evaluators

- `MIN_GC_TIME` (default) - Selects the process with the lowest total GC time per request (`GCT / request count`).
- `MIN_FULL_GC_COUNT` - Selects the process with the lowest Full GC time per request (`FGCT / request count`).

### Triggering feedback

The feedback loop (evaluate current processes and adjust parameters) runs on refresh. Refresh can be triggered by:

1. **`--lifetime` option** - Automatic periodic refresh at the specified interval.
2. **HUP signal** - `kill -HUP <container_pid>`
3. **REST API** - `curl -X POST http://localhost:44010/container/refresh`

### Observability

Auto Tuning logs the following information:

**Process startup** - JVM options applied to each process

```text
process started: id=abc12, pid=12345, jvmOptions=[-server, -Xms128m, -Xmx128m]
```

**Feedback evaluation** - Score comparison of all processes

```text
process id=abc12, jvmOptions=[-server, -Xms256m, -Xmx256m], score(GCT/requests)=1.0E-4
process id=def34, jvmOptions=[-server, -Xms64m, -Xmx64m], score(GCT/requests)=0.005
best param [-server, -Xms256m, -Xmx256m]
```

**Shutdown summary** - Tuning history across all rounds

```text
=== Auto Tuning Summary (3 rounds) ===
  Round 1: [-server, -Xms128m, -Xmx128m]
  Round 2: [-server, -Xms192m, -Xmx192m]
  Round 3: [-server, -Xms256m, -Xmx256m]
  Final:   [-server, -Xms256m, -Xmx256m]
============================================
```

## Spring Boot Example

The `falchion-example-spring-boot` module demonstrates how to integrate Falchion with a Spring Boot application using SO_REUSEPORT and Auto Tuning.

### Key components

- **ReusePortJettyCustomizer** - Replaces Jetty's default connector with `ReusePortConnector` for SO_REUSEPORT support
- **FalchionMetricsFilter** - Servlet filter that records Dropwizard `falchion.requests` Timer for `MetricsJmxMonitor`
- **Application** - Notifies the Falchion container of readiness via POST to `http://localhost:44010/jvm/{pid}/ready`

### Build and run

```bash
# Build
mvn clean package -pl falchion-container,falchion-jetty9-connector,falchion-example-spring-boot

# Run with Falchion Auto Tuning
java -jar falchion-container/target/falchion-container-0.3.0.jar \
  net.unit8.falchion.example.springboot.Application \
  --auto-tuning \
  -m GCUTIL_JSTAT METRICS_JMX \
  -p 2 \
  --lifetime 300 \
  -cp "falchion-example-spring-boot/target/classes:falchion-example-spring-boot/target/lib/*"
```

### Load testing

```bash
cd falchion-loadtest
mvn gatling:test
```

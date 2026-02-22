package net.unit8.falchion;

import net.unit8.falchion.evaluator.EvaluatorSupplier;
import net.unit8.falchion.monitor.MonitorSupplier;
import net.unit8.falchion.webhook.WebhookNotifier;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.Messages;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bootstraps JVM container.
 *
 * @author kawasima
 */
public class Bootstrap {
    private static final Logger LOG = LoggerFactory.getLogger(Bootstrap.class);
    private static final Signal HUP  = new Signal("HUP");
    private static final Signal TERM = new Signal("TERM");

    @Option(name = "-cp", usage = "Class search path of directories and zip/jar files",
            metaVar = "CLASSPATH")
    private String classpath;

    @Option(name = "-m", usage = "name of JVM process monitor", handler = StringArrayOptionHandler.class)
    private String[] monitors = {};

    @Option(name = "-p", usage = "size of JVM processes", metaVar = "SIZE")
    private int poolSize = 1;

    @Option(name = "--admin-port", usage = "a port number of the api server", metaVar = "PORT")
    private int adminPort = 44010;

    @Option(name = "--auto-tuning", usage = "tuning JVM parameter automatically")
    private boolean autoTuning = false;

    @Option(name = "--evaluator", usage ="JVM parameter evaluator")
    private String evaluator;

    @Option(name = "--lifetime", usage = "lifetime of a jvm process",
            metaVar = "SEC")
    private long lifetime = 0;

    @Option(name = "-basedir", usage = "base directory of zip/jar files")
    private String basedir;

    @Option(name = "-v", usage = "application version")
    private String aplVersion;

    @Option(name = "--java-opts", usage = "options for worker processes",
            metaVar = "JAVA_OPTS")
    private String javaOpts;

    @Option(name = "--variance", usage = "variance for auto tuning parameter sampling (default: 0.1)")
    private double variance = 0.1;

    @Option(name = "--config", usage = "configuration file path", metaVar = "FILE")
    private String configFile;

    @Option(name = "--health-check-path", usage = "HTTP health check endpoint path",
            metaVar = "PATH")
    private String healthCheckPath;

    @Option(name = "--health-check-interval", usage = "health check interval in seconds (default: 30)",
            metaVar = "SEC")
    private long healthCheckInterval = 30;

    @Option(name = "--app-port", usage = "application port for health check (default: 8080)",
            metaVar = "PORT")
    private int appPort = 8080;

    @Option(name = "--gc-algorithm", usage = "GC algorithm (PARALLEL, G1, ZGC, SHENANDOAH). If not set with auto-tuning, random selection is used.")
    private String gcAlgorithm;

    @Option(name = "--webhook-url", usage = "URL for lifecycle event notifications", metaVar = "URL")
    private String webhookUrl;

    @Option(name = "--drain-path", usage = "HTTP endpoint path to notify before killing a process",
            metaVar = "PATH")
    private String drainPath;

    @Option(name = "--drain-timeout", usage = "seconds to wait after drain request before killing (default: 30)",
            metaVar = "SEC")
    private long drainTimeout = 30;

    /** @noinspection MismatchedQueryAndUpdateOfCollection*/
    @Argument
    private List<String> arguments = new ArrayList<>();

    private void loadConfig(String configPath) {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configPath)) {
            props.load(fis);
        } catch (IOException e) {
            LOG.error("Failed to load config file: {}", configPath, e);
            return;
        }
        LOG.info("Loaded config from {}", configPath);

        if (props.containsKey("pool-size")) poolSize = Integer.parseInt(props.getProperty("pool-size"));
        if (props.containsKey("admin-port")) adminPort = Integer.parseInt(props.getProperty("admin-port"));
        if (props.containsKey("auto-tuning")) autoTuning = Boolean.parseBoolean(props.getProperty("auto-tuning"));
        if (props.containsKey("evaluator")) evaluator = props.getProperty("evaluator");
        if (props.containsKey("lifetime")) lifetime = Long.parseLong(props.getProperty("lifetime"));
        if (props.containsKey("basedir")) basedir = props.getProperty("basedir");
        if (props.containsKey("version")) aplVersion = props.getProperty("version");
        if (props.containsKey("java-opts")) javaOpts = props.getProperty("java-opts");
        if (props.containsKey("classpath")) classpath = props.getProperty("classpath");
        if (props.containsKey("variance")) variance = Double.parseDouble(props.getProperty("variance"));
        if (props.containsKey("monitors")) monitors = props.getProperty("monitors").split(",");
        if (props.containsKey("health-check-path")) healthCheckPath = props.getProperty("health-check-path");
        if (props.containsKey("health-check-interval")) healthCheckInterval = Long.parseLong(props.getProperty("health-check-interval"));
        if (props.containsKey("app-port")) appPort = Integer.parseInt(props.getProperty("app-port"));
        if (props.containsKey("gc-algorithm")) gcAlgorithm = props.getProperty("gc-algorithm");
        if (props.containsKey("webhook-url")) webhookUrl = props.getProperty("webhook-url");
        if (props.containsKey("drain-path")) drainPath = props.getProperty("drain-path");
        if (props.containsKey("drain-timeout")) drainTimeout = Long.parseLong(props.getProperty("drain-timeout"));
    }

    public void doMain(String... args) {
        // Pre-scan for --config to load defaults before args4j parsing
        for (int i = 0; i < args.length - 1; i++) {
            if ("--config".equals(args[i])) {
                loadConfig(args[i + 1]);
                break;
            }
        }

        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
            if (arguments.isEmpty()) {
                throw new CmdLineException(parser, Messages.DEFAULT_META_EXPLICIT_BOOLEAN_OPTION_HANDLER);
            }
        } catch (CmdLineException ex) {
            ex.printStackTrace();
            parser.printUsage(System.err);
            return;
        }

        LOG.info("monitors={}", Arrays.asList(monitors));
        Set<MonitorSupplier> monitorSuppliers = Arrays.stream(monitors)
                .map(MonitorSupplier::valueOf)
                .collect(Collectors.toSet());

        Container container = new Container(poolSize);
        container.setMonitorSuppliers(monitorSuppliers);
        container.setAutoTuning(autoTuning);
        container.setBasedir(basedir);
        if (evaluator != null) {
            container.setEvaluator(EvaluatorSupplier.valueOf(evaluator).createEvaluator());
        } else if (autoTuning) {
            container.setEvaluator(EvaluatorSupplier.MIN_GC_TIME.createEvaluator());
        }
        container.setJavaOpts(javaOpts);
        container.setVariance(variance);
        container.setGcAlgorithm(gcAlgorithm);
        if (webhookUrl != null) {
            container.setWebhookNotifier(new WebhookNotifier(webhookUrl));
        }
        if (drainPath != null) {
            container.setDrainPath(drainPath);
            container.setDrainTimeout(drainTimeout);
        }
        container.setAppPort(appPort);
        if (healthCheckPath != null) {
            container.setHealthCheckPath(healthCheckPath);
            container.setHealthCheckInterval(healthCheckInterval);
        }
        if (lifetime > 0) {
            container.setLifetime(lifetime);
        }
        ApiServer apiServer = new ApiServer(container, adminPort);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutdown hook triggered");
            if (container.getHealthChecker() != null) {
                container.getHealthChecker().stop();
            }
            if (container.getPool() != null) {
                container.getPool().shutdown();
            }
            apiServer.stop();
        }));

        Signal.handle(TERM, signal -> {
            container.getPool().shutdown();
            apiServer.stop();
        });
        Signal.handle(HUP, signal -> {
            try {
                container.getPool().refresh();
            } catch (IOException ex) {
                LOG.error("Refreshing the JVM pool is failure", ex);
            }
        });

        apiServer.start();
        if (Objects.nonNull(classpath)) {
            container.start(args[0], classpath);
        } else if (Objects.nonNull(basedir) && Objects.nonNull(aplVersion)) {
            container.start(args[0], basedir, aplVersion);
        } else {
            container.start(args[0]);
        }
    }

    public static void main(String... args) {
        int version = Runtime.version().feature();
        if (version < 17) {
            throw new UnsupportedOperationException("JDK 17 or newer is required");
        }
        new Bootstrap().doMain(args);
    }
}

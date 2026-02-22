package net.unit8.falchion;

import net.unit8.falchion.evaluator.EvaluatorSupplier;
import net.unit8.falchion.monitor.MonitorSupplier;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.Messages;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;

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

    /** @noinspection MismatchedQueryAndUpdateOfCollection*/
    @Argument
    private List<String> arguments = new ArrayList<>();

    public void doMain(String... args) {
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
        if (lifetime > 0) {
            container.setLifetime(lifetime);
        }
        ApiServer apiServer = new ApiServer(container, adminPort);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutdown hook triggered");
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
        double version = Double.parseDouble(System.getProperty("java.specification.version"));
        if (version < 9) {
            throw new UnsupportedOperationException("JDK9 or newer is required");
        }
        new Bootstrap().doMain(args);
    }
}

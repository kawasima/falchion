package net.unit8.falchion;

import net.unit8.falchion.monitor.MonitorSupplier;
import org.kohsuke.args4j.*;
import org.kohsuke.args4j.spi.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author kawasima
 */
public class Bootstrap {
    private static final Logger LOG = LoggerFactory.getLogger(Bootstrap.class);
    private static final Signal HUP  = new Signal("HUP");
    private static final Signal TERM = new Signal("TERM");

    @Option(name = "-cp", usage="Class search path of directories and zip/jar files",
            metaVar = "CLASSPATH")
    private String classpath;

    @Option(name = "-m")
    private List<String> monitors = new ArrayList<>();

    @Option(name = "-p", usage = "size of JVM processes", metaVar = "SIZE")
    private int poolSize = 1;

    @Option(name = "--auto-tuning", usage = "tuning JVM parameter automatically")
    private boolean autoTuning = false;

    @Option(name = "--lifetime", usage = "lifetime of a jvm process",
            metaVar = "SEC")
    private long lifetime = 0;

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
            parser.printUsage(System.err);
            return;
        }

        Set<MonitorSupplier> monitorSuppliers = monitors.stream()
                .map(MonitorSupplier::valueOf)
                .collect(Collectors.toSet());

        Container container = new Container(poolSize);
        container.setMonitorSuppliers(monitorSuppliers);
        if (lifetime > 0) {
            container.setLifetime(lifetime);
        }
        ApiServer apiServer = new ApiServer(container);

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
        if (classpath == null) {
            container.start(args[0]);
        } else {
            container.start(args[0], classpath);
        }
    }

    public static void main(String... args) {
        new Bootstrap().doMain(args);
    }
}

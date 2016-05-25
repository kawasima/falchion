package net.unit8.falchion;

import net.unit8.falchion.evaluator.Evaluator;
import net.unit8.falchion.monitor.MonitorSupplier;
import net.unit8.falchion.supplier.AutoOptimizableProcessSupplier;
import net.unit8.falchion.supplier.StandardProcessSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Manages some JVM processes.
 *
 * @author kawasima
 */
public class Container {
    private static final Logger LOG = LoggerFactory.getLogger(Container.class);
    private int poolSize;
    private JvmPool pool;
    private File logDir;
    private long lifetime;
    private boolean autoTuning;
    private Evaluator evaluator;
    private String javaOpts;

    private ScheduledExecutorService autoRefreshTimer;

    private Set<MonitorSupplier> monitorSuppliers;

    /**
     * Creates a container.
     *
     * @param poolSize The size of JVM pool.
     */
    public Container(int poolSize) {
        this.poolSize = poolSize;
    }

    /**
     * Gets the classpath that is applied for a child process.
     *
     * @return a String contains classpath
     */
    private String getClasspath() {
        LOG.info(System.getProperty("java.class.path"));
        return Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .map(File::new)
                .map(File::getAbsolutePath)
                .collect(Collectors.joining(":"));
    }

    /**
     * Starts the container.
     *
     * @param mainClass the name of main class
     * @param classpath the classpath is applied for a child process
     */
    public void start(final String mainClass, String classpath) {
        Supplier<JvmProcess> processSupplier = new StandardProcessSupplier(
                mainClass, classpath, javaOpts, logDir, monitorSuppliers);
        if (autoTuning) {
            LOG.info("Auto tuning setup using evaluator {}", evaluator);
            processSupplier = new AutoOptimizableProcessSupplier(processSupplier, evaluator);
        }
        pool = new JvmPool(poolSize, processSupplier);
        pool.fill();
        if (lifetime > 0) {
            autoRefreshTimer = Executors.newSingleThreadScheduledExecutor();
            autoRefreshTimer.scheduleAtFixedRate(() -> {
                try {
                    pool.refresh();
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }, lifetime, lifetime, TimeUnit.SECONDS);
        }
    }

    public void start(final String mainClass) {
        start(mainClass, getClasspath());
    }

    /**
     * Returns the pool of JVM processes.
     *
     * @return a JvmPool
     */
    public JvmPool getPool() {
        return pool;
    }

    public void setLogDir(File logDir) {
        this.logDir = logDir;
    }

    public void setMonitorSuppliers(Set<MonitorSupplier> monitorSuppliers) {
        this.monitorSuppliers = monitorSuppliers;
    }

    public void setLifetime(long survivalTime) {
        this.lifetime = survivalTime;
    }

    public void setJavaOpts(String javaOpts) {
        this.javaOpts = javaOpts;
    }

    public void setAutoTuning(boolean autoTuning) {
        this.autoTuning = autoTuning;
    }

    public void setEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
    }
}

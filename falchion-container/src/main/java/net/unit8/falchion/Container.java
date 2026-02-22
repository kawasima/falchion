package net.unit8.falchion;

import net.unit8.falchion.evaluator.Evaluator;
import net.unit8.falchion.health.HealthChecker;
import net.unit8.falchion.monitor.MonitorSupplier;
import net.unit8.falchion.option.GcAlgorithm;
import net.unit8.falchion.supplier.AutoOptimizableProcessSupplier;
import net.unit8.falchion.supplier.StandardProcessSupplier;
import net.unit8.falchion.webhook.WebhookNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

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
    private String basedir;
    private double variance = 0.1;
    private String healthCheckPath;
    private int appPort = 8080;
    private long healthCheckInterval = 30;
    private String gcAlgorithm;
    private WebhookNotifier webhookNotifier;
    private String drainPath;
    private long drainTimeout = 30;

    private ScheduledExecutorService autoRefreshTimer;
    private HealthChecker healthChecker;

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
                .collect(joining(":"));
    }

    /**
     * Starts the container.
     *
     * @param mainClass the name of main class
     * @param classpath the classpath is applied for a child process
     */
    public void start(final String mainClass, String classpath) {
        StandardProcessSupplier stdSupplier = new StandardProcessSupplier(
                mainClass, classpath, javaOpts, logDir, monitorSuppliers);
        if (gcAlgorithm != null) {
            stdSupplier.setGcAlgorithm(GcAlgorithm.valueOf(gcAlgorithm));
        }
        Supplier<JvmProcess> processSupplier = stdSupplier;
        if (autoTuning) {
            boolean autoSelectGc = (gcAlgorithm == null);
            LOG.info("Auto tuning setup using evaluator {}, autoSelectGc={}", evaluator, autoSelectGc);
            processSupplier = new AutoOptimizableProcessSupplier(processSupplier, evaluator, variance, autoSelectGc);
        }
        if (gcAlgorithm != null) {
            LOG.info("GC algorithm fixed to {}", gcAlgorithm);
        }
        pool = new JvmPool(poolSize, processSupplier);
        pool.setWebhookNotifier(webhookNotifier);
        if (drainPath != null) {
            pool.setDrainConfig(drainPath, appPort, drainTimeout);
        }
        pool.fill();
        if (healthCheckPath != null) {
            healthChecker = new HealthChecker(healthCheckPath, appPort, healthCheckInterval,
                    pool::getActiveProcesses, process -> {
                try {
                    process.kill();
                } catch (IOException e) {
                    LOG.warn("Failed to kill unhealthy process id={}", process.getId(), e);
                }
            });
            healthChecker.start();
        }
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

    /**
     * start the container. this method generates classpath using basedir and aplVersion.
     *
     * @param mainClass  the name of main class
     * @param basedir    the base directory where application zip/jar is stored
     * @param aplVersion application version
     */
    public void start(final String mainClass, String basedir, String aplVersion) {
        start(mainClass, createClasspath(basedir, aplVersion));
    }

    public void start(final String mainClass) {
        start(mainClass, getClasspath());
    }

    /**
     * generates classpath using basedir and aplVersion.
     *
     * @param basedir    the base directory where application zip/jar is stored
     * @param aplVersion application version
     */
    public String createClasspath(String basedir, String aplVersion) {
        try {
            Stream<Path> paths = Files.walk(Paths.get(basedir));
            String jarsPath = paths
                    .filter(path -> path.toFile().isDirectory() && path.toFile().getName().contains(aplVersion))
                    .flatMap(path -> {
                        try {
                            return Files.walk(Paths.get(path.toFile().getAbsolutePath()));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .filter(path -> path.toFile().isFile() && path.toFile().getName().endsWith(".jar"))
                    .map(path -> path.toFile().getAbsolutePath())
                    .collect(joining(":"));
            if (jarsPath.isEmpty()) {
                return basedir;
            } else {
                return basedir + ":" + jarsPath;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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

    public void setVariance(double variance) {
        this.variance = variance;
    }

    public double getVariance() {
        return variance;
    }

    public void setHealthCheckPath(String healthCheckPath) {
        this.healthCheckPath = healthCheckPath;
    }

    public void setAppPort(int appPort) {
        this.appPort = appPort;
    }

    public void setHealthCheckInterval(long healthCheckInterval) {
        this.healthCheckInterval = healthCheckInterval;
    }

    public HealthChecker getHealthChecker() {
        return healthChecker;
    }

    public int getAppPort() {
        return appPort;
    }

    public void setWebhookNotifier(WebhookNotifier webhookNotifier) {
        this.webhookNotifier = webhookNotifier;
    }

    public void setDrainPath(String drainPath) {
        this.drainPath = drainPath;
    }

    public void setDrainTimeout(long drainTimeout) {
        this.drainTimeout = drainTimeout;
    }

    public void setGcAlgorithm(String gcAlgorithm) {
        this.gcAlgorithm = gcAlgorithm;
    }

    public void setBasedir(String basedir) {
        this.basedir = basedir;
    }

    public String getBasedir() {
        return basedir;
    }
}

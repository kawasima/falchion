package net.unit8.falchion;

import net.unit8.falchion.monitor.MonitorSupplier;
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
import java.util.stream.Collectors;

/**
 * @author kawasima
 */
public class Container {
    private static final Logger LOG = LoggerFactory.getLogger(Container.class);
    private int poolSize;
    private JvmPool pool;
    private File logDir;
    private long lifetime;
    private ScheduledExecutorService autoRefreshTimer;

    private Set<MonitorSupplier> monitorSuppliers;

    public Container(int poolSize) {
        this.poolSize = poolSize;
    }

    private String getClasspath() {
        LOG.info(System.getProperty("java.class.path"));
        return Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .map(File::new)
                .map(File::getAbsolutePath)
                .collect(Collectors.joining(":"));
    }

    public void start(final String mainClass, String classpath) {
        pool = new JvmPool(poolSize, () -> {
            JvmProcess p = new JvmProcess(mainClass, classpath);
            if (logDir != null && logDir.isDirectory()) {
                p.setIoDir(logDir);
            }
            monitorSuppliers.forEach(ms -> p.addMonitor(ms.createMonitor()));
            return p;
        });
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
}

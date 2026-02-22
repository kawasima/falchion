package net.unit8.falchion.health;

import net.unit8.falchion.JvmProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Polls HTTP health check endpoint for each JVM process.
 *
 * @author kawasima
 */
public class HealthChecker {
    private static final Logger LOG = LoggerFactory.getLogger(HealthChecker.class);

    private final String healthCheckPath;
    private final int appPort;
    private final long intervalSeconds;
    private final int failureThreshold;
    private final Supplier<List<JvmProcess>> processesSupplier;
    private final Consumer<JvmProcess> onUnhealthy;
    private final Map<String, Integer> failureCounts = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;

    public HealthChecker(String healthCheckPath, int appPort, long intervalSeconds,
                         Supplier<List<JvmProcess>> processesSupplier,
                         Consumer<JvmProcess> onUnhealthy) {
        this.healthCheckPath = healthCheckPath;
        this.appPort = appPort;
        this.intervalSeconds = intervalSeconds;
        this.failureThreshold = 3;
        this.processesSupplier = processesSupplier;
        this.onUnhealthy = onUnhealthy;
    }

    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "health-checker");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::checkAll, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        LOG.info("Health checker started: path={}, port={}, interval={}s", healthCheckPath, appPort, intervalSeconds);
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    private void checkAll() {
        List<JvmProcess> processes = processesSupplier.get();
        for (JvmProcess process : processes) {
            boolean healthy = check(process);
            if (healthy) {
                failureCounts.remove(process.getId());
            } else {
                int count = failureCounts.merge(process.getId(), 1, Integer::sum);
                LOG.warn("Health check failed for process id={}, pid={}, failures={}/{}",
                        process.getId(), process.getPid(), count, failureThreshold);
                if (count >= failureThreshold) {
                    LOG.error("Process id={}, pid={} is unhealthy after {} consecutive failures, killing",
                            process.getId(), process.getPid(), count);
                    failureCounts.remove(process.getId());
                    onUnhealthy.accept(process);
                }
            }
        }
    }

    private boolean check(JvmProcess process) {
        try {
            URL url = new URL("http://localhost:" + appPort + healthCheckPath);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            try {
                int responseCode = conn.getResponseCode();
                return responseCode >= 200 && responseCode < 400;
            } finally {
                conn.disconnect();
            }
        } catch (IOException e) {
            LOG.debug("Health check connection failed for process id={}: {}", process.getId(), e.getMessage());
            return false;
        }
    }
}

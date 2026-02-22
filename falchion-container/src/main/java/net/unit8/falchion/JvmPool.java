package net.unit8.falchion;

import net.unit8.falchion.supplier.AutoOptimizableProcessSupplier;
import net.unit8.falchion.webhook.WebhookNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author kawasima
 */
public class JvmPool {
    private static final Logger LOG = LoggerFactory.getLogger(JvmPool.class);

    private boolean autofill = true;
    private int poolSize;
    private Supplier<JvmProcess> processSupplier;
    private ExecutorService completionMonitorService;
    private ExecutorService jvmProcessService;
    private ExecutorCompletionService<JvmResult> jvmCompletionService;

    private final AtomicBoolean shutdownCalled = new AtomicBoolean(false);
    private Map<String, ProcessHolder> processes = new ConcurrentHashMap<>();
    private String classpath;
    private WebhookNotifier webhookNotifier;
    private String drainPath;
    private int drainPort;
    private long drainTimeout;

    public JvmPool(int poolSize, Supplier<JvmProcess> processSupplier) {
        this.poolSize = poolSize;
        this.processSupplier = processSupplier;

        completionMonitorService = Executors.newSingleThreadExecutor();
        jvmProcessService = Executors.newCachedThreadPool();
        jvmCompletionService = new ExecutorCompletionService<>(jvmProcessService);

        completionMonitorService.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Future<JvmResult> resultFuture = jvmCompletionService.take();
                    JvmResult result = resultFuture.get();
                    LOG.info("JVM end (id = {}, status={})", result.getId(), result.getExitStatus());
                    processes.remove(result.getId());
                } catch (InterruptedException ex) {
                    LOG.error("Completion monitor interrupted", ex);
                } catch (ExecutionException ex) {
                    LOG.error("Process ends with error", ex);
                }
                if (autofill) fill();
            }
        });
    }

    public JvmProcess create() {
        JvmProcess process = processSupplier.get();
        if (Objects.nonNull(classpath)) {
            process.setClasspath(classpath);
        }
        Future<JvmResult> future = jvmCompletionService.submit(process);
        processes.put(process.getId(), new ProcessHolder(process, future));
        LOG.info("create new JVM (id={}, pid={})", process.getId(), process.getPid());
        if (webhookNotifier != null) {
            webhookNotifier.notifyProcessStarted(process.getId(), process.getPid());
        }
        return process;
    }

    public void fill() {
        while (processes.size() < poolSize) {
            create();
        }
    }

    public void refresh() throws IOException {
        if (webhookNotifier != null) {
            webhookNotifier.notifyRefreshStarted();
        }

        Set<JvmProcess> oldProcesses = processes.values().stream()
                .map(ProcessHolder::getProcess)
                .collect(Collectors.toSet());

        // If process supplier is instance of AutoOptimizableProcessSupplier,
        // feedback current monitoring value to supplier.
        Stream.of(processSupplier)
                .filter(AutoOptimizableProcessSupplier.class::isInstance)
                .map(AutoOptimizableProcessSupplier.class::cast)
                .forEach(supplier -> supplier.feedback(oldProcesses));

        for (JvmProcess oldProcess : oldProcesses) {
            try {
                JvmProcess process = create();
                process.waitForReady().get();
            } catch (Exception e) {
                LOG.warn("fail to create a new process");
                throw new IOException(e);
            }
            if (drainPath != null) {
                drain(oldProcess);
            }
            if (webhookNotifier != null) {
                webhookNotifier.notifyProcessStopped(oldProcess.getId(), oldProcess.getPid());
            }
            oldProcess.kill();
        }

        if (webhookNotifier != null) {
            webhookNotifier.notifyRefreshCompleted();
        }
    }

    public List<JvmProcess> getActiveProcesses() {
        return processes.values().stream()
                .map(ProcessHolder::getProcess)
                .collect(Collectors.toList());
    }
    public void shutdown() {
        if (!shutdownCalled.compareAndSet(false, true)) {
            return;
        }
        LOG.info("Pool shutdown begin");

        Stream.of(processSupplier)
                .filter(AutoOptimizableProcessSupplier.class::isInstance)
                .map(AutoOptimizableProcessSupplier.class::cast)
                .forEach(AutoOptimizableProcessSupplier::printTuningSummary);

        jvmProcessService.shutdown();
        processes.values().forEach(p -> p.getFuture().cancel(true));
        completionMonitorService.shutdown();
        if (webhookNotifier != null) {
            webhookNotifier.shutdown();
        }
        LOG.info("Pool shutdown end");
    }

    public void info() {
        LOG.info("Pool Size=" + processes.size());
    }

    public JvmProcess getProcess(String id) {
        return Optional.ofNullable(processes.get(id))
                .map(ProcessHolder::getProcess)
                .orElse(null);
    }

    public JvmProcess getProcessByPid(long pid) {
        return processes.values().stream()
                .map(ProcessHolder::getProcess)
                .filter(p -> p != null && p.getPid() == pid)
                .findAny()
                .orElse(null);
    }

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    public void setWebhookNotifier(WebhookNotifier webhookNotifier) {
        this.webhookNotifier = webhookNotifier;
    }

    public void setDrainConfig(String drainPath, int drainPort, long drainTimeout) {
        this.drainPath = drainPath;
        this.drainPort = drainPort;
        this.drainTimeout = drainTimeout;
        LOG.info("Drain configured: path={}, port={}, timeout={}s", drainPath, drainPort, drainTimeout);
    }

    private void drain(JvmProcess process) {
        try {
            URL url = new URL("http://localhost:" + drainPort + drainPath);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            try {
                int responseCode = conn.getResponseCode();
                LOG.info("Drain request sent to process id={}, pid={}, response={}", process.getId(), process.getPid(), responseCode);
            } finally {
                conn.disconnect();
            }
        } catch (IOException e) {
            LOG.warn("Drain request failed for process id={}: {}", process.getId(), e.getMessage());
        }

        try {
            LOG.info("Waiting {}s for drain to complete for process id={}", drainTimeout, process.getId());
            TimeUnit.SECONDS.sleep(drainTimeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Drain wait interrupted for process id={}", process.getId());
        }
    }

    static class ProcessHolder {
        private JvmProcess process;
        private Future<JvmResult> future;

        ProcessHolder(JvmProcess process, Future<JvmResult> future) {
            this.process = process;
            this.future = future;
        }

        JvmProcess getProcess() {
            return process;
        }

        Future<JvmResult> getFuture() {
            return future;
        }
    }
}

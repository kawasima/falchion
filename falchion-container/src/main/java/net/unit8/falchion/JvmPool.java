package net.unit8.falchion;

import net.unit8.falchion.supplier.AutoOptimizableProcessSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
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

    private Map<String, ProcessHolder> processes = new ConcurrentHashMap<>();
    private String classpath;

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
        return process;
    }

    public void fill() {
        while (processes.size() < poolSize) {
            create();
        }
    }

    public void refresh() throws IOException {
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
            oldProcess.kill();
        }
    }

    public List<JvmProcess> getActiveProcesses() {
        return processes.values().stream()
                .map(ProcessHolder::getProcess)
                .collect(Collectors.toList());
    }
    public void shutdown() {
        LOG.info("Pool shutdown begin");
        jvmProcessService.shutdown();
        processes.values().forEach(p -> p.getFuture().cancel(true));
        completionMonitorService.shutdown();
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

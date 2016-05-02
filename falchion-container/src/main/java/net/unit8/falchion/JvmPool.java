package net.unit8.falchion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

    public JvmPool(int poolSize, Supplier<JvmProcess> processSupplier) {
        this.poolSize = poolSize;
        this.processSupplier = processSupplier;

        completionMonitorService = Executors.newSingleThreadExecutor();
        jvmProcessService = Executors.newCachedThreadPool();
        jvmCompletionService = new ExecutorCompletionService<>(jvmProcessService);

        completionMonitorService.submit(() -> {
            while (true) {
                try {
                    Future<JvmResult> resultFuture = jvmCompletionService.take();
                    JvmResult result = resultFuture.get();
                    LOG.info("JVM end (id = " + result.getId()
                            + ", status=" + result.getExitStatus() + ")");
                    processes.remove(result.getId());
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                } finally {

                }
                if (autofill) fill();
            }
        });
    }

    public JvmProcess create() {
        JvmProcess process = processSupplier.get();
        Future<JvmResult> future = jvmCompletionService.submit(process);
        processes.put(process.getId(), new ProcessHolder(process, future));
        LOG.info("create new JVM (id=" + process.getId()
                + ", pid=" + process.getPid()
                + ")");
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
        return processes.get(id).getProcess();
    }

    public JvmProcess getProcessByPid(long pid) {
        return processes.values().stream()
                .map(ProcessHolder::getProcess)
                .filter(p -> p.getPid() == pid)
                .findFirst()
                .orElse(null);
    }

    static class ProcessHolder {
        private JvmProcess process;
        private Future<JvmResult> future;

        public ProcessHolder(JvmProcess process, Future<JvmResult> future) {
            this.process = process;
            this.future = future;
        }

        public JvmProcess getProcess() {
            return process;
        }

        public Future<JvmResult> getFuture() {
            return future;
        }
    }
}
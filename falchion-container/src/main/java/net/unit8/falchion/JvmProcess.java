package net.unit8.falchion;

import net.unit8.falchion.monitor.JvmMonitor;
import net.unit8.falchion.monitor.MonitorStat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author kawasima
 */
public class JvmProcess implements Callable<JvmResult> {
    private static final Logger LOG = LoggerFactory.getLogger(JvmProcess.class);

    private static final DateTimeFormatter fmtIO = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private String id;
    private long pid = -1;
    private String mainClass;
    private String classpath;
    private List<String> jvmOptions;

    private ProcessBuilder processBuilder;
    private transient Process process;
    private long startedAt;
    private CompletableFuture<Void> ready;

    private Set<JvmMonitor> monitors;

    private static String ID_CHARS = "0123456789abcdefghijklmnopqrstuvwxyz";

    private String generateId(int length) {
        return new Random().ints(0, ID_CHARS.length())
                .mapToObj(i -> ID_CHARS.charAt(i))
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    public JvmProcess(String mainClass, String classpath) {
        this.id = generateId(5);
        this.mainClass = mainClass;
        this.classpath = classpath;
        this.ready = new CompletableFuture<>();
        this.monitors = new HashSet<>();

        processBuilder = new ProcessBuilder().inheritIO();
    }

    public CompletableFuture<Void> waitForReady() {
        return ready;
    }

    public void ready() {
        ready.complete(null);
        monitors.forEach(m -> m.start(this));
    }

    @Override
    public JvmResult call() throws Exception {
        String javaHome = System.getProperty("java.home");
        List<String> commandArgs = new ArrayList<>();
        commandArgs.addAll(Arrays.asList(javaHome + "/bin/java", "-cp", classpath));
        if (jvmOptions != null) {
            commandArgs.addAll(jvmOptions);
        }
        commandArgs.add(mainClass);
        processBuilder.command(commandArgs);
        try {
            process = processBuilder.start();
            startedAt = System.currentTimeMillis();
            pid = process.getPid();
            LOG.info("process started: id={}, pid={}", id, pid);

            return new JvmResult(id, pid, process.waitFor());
        } catch (InterruptedException ex) {
            LOG.info("process interrupted: id={}, pid={}", id, pid);
            try {
                process.waitFor(3, TimeUnit.SECONDS);
            } catch (InterruptedException ignore) {
            }
            process.destroy();
            LOG.info("process destroy: id={}, pid={}", id, pid);
            return new JvmResult(id, pid, -1);
        } catch (Exception ex) {
            LOG.error("Process start failure", ex);
            throw ex;
        } finally {
            monitors.forEach(JvmMonitor::stop);

            if (process != null) {
                process.getInputStream().close();
                process.getOutputStream().close();
                process.getErrorStream().close();
            }
        }
    }

    public String getId() {
        return id;
    }

    public long getPid() {
        return pid;
    }

    public long getUptime(){
        return System.currentTimeMillis() - startedAt;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void addMonitor(JvmMonitor... monitors) {
        this.monitors.addAll(Arrays.asList(monitors));
    }

    public void kill() throws IOException {
        try {
            Process killProcess = new ProcessBuilder("kill", "-TERM", Long.toString(pid)).start();
            int killResult = killProcess.waitFor();
            if (killResult != 0)
                throw new IOException("kill " + pid + " is failure");
        } catch (InterruptedException ex) {
            LOG.warn("Kill the process (pid={}) is canceled.", pid);
        }
    }

    public void setIoDir(File directory) {
        if (process != null) {
            throw new IllegalStateException("You should call setIoDir before starting process");
        }
        String prefix = fmtIO.format(LocalDateTime.now()) + "." + id;
        processBuilder
                .redirectError(new File(directory, prefix + ".err"))
                .redirectOutput(new File(directory, prefix + ".out"));
    }

    public List<MonitorStat> getMonitorStats() {
        return monitors.stream()
                .map(JvmMonitor::getStat)
                .collect(Collectors.toList());
    }

    public void setJvmOptions(List<String> options) {
        this.jvmOptions = options;
    }

    public List<String> getJvmOptions() {
        return jvmOptions;
    }

    void setClasspath(String classpath) {
        this.classpath = classpath;
    }

}

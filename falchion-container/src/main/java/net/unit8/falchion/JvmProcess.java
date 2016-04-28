package net.unit8.falchion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author kawasima
 */
public class JvmProcess implements Callable<JvmResult> {
    private static final Logger LOG = LoggerFactory.getLogger(JvmProcess.class);

    private String id;
    private long pid = -1;
    private String mainClass;
    private ProcessBuilder processBuilder;
    private transient Process process;
    private long startedAt;
    private CompletableFuture<Void> ready;

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
        this.ready = new CompletableFuture<>();
        String javaHome = System.getProperty("java.home");
        LOG.info(javaHome + "/bin/java " + "-cp " + classpath + " " + mainClass);
        processBuilder = new ProcessBuilder()
                .command(javaHome + "/bin/java",
                        "-cp", classpath,
                        mainClass)
                .inheritIO();
    }

    public CompletableFuture<Void> waitForReady() {
        return ready;
    }

    public void ready() {
        ready.complete(null);
    }

    @Override
    public JvmResult call() throws Exception {
        try {
            process = processBuilder.start();
            startedAt = System.currentTimeMillis();
            pid = process.getPid();

            LOG.info("process started: id=" + id + ", pid=" + pid);
            return new JvmResult(id, pid, process.waitFor());
        } catch (InterruptedException ex) {
            LOG.info("process interrupted: id=" + id + ", pid=" + pid);
            try {
                process.waitFor(3, TimeUnit.SECONDS);
            } catch (InterruptedException ignore) {
            }
            process.destroy();
            LOG.info("process destroy: id=" + id + ", pid=" + pid);
            return new JvmResult(id, pid, -1);
        } catch (Exception ex) {
            LOG.error("Process start failure", ex);
            throw ex;
        } finally {
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

    public void kill() throws IOException {
        try {
            Process killProcess = new ProcessBuilder("kill", "-TERM", Long.toString(pid)).start();
            int killResult = killProcess.waitFor();
            if (killResult != 0)
                throw new IOException("kill " + pid + " is failure");
        } catch (InterruptedException ex) {
            LOG.info("Kill the process (pid={}) is canceled.", pid);
        }
    }

}

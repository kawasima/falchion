package net.unit8.falchion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.Callable;
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

    public JvmProcess(String mainClass, String classpath) {
        this.id = UUID.randomUUID().toString();
        this.mainClass = mainClass;
        String javaHome = System.getProperty("java.home");
        LOG.info(javaHome + "/bin/java " + "-cp " + classpath + " " + mainClass);
        processBuilder = new ProcessBuilder()
                .command(javaHome + "/bin/java",
                        "-cp", classpath,
                        mainClass)
                .inheritIO();
    }

    @Override
    public JvmResult call() throws Exception {
        try {
            process = processBuilder.start();
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
            throw ex;
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

    public String getMainClass() {
        return mainClass;
    }
}

package net.unit8.falchion.monitor;

import net.unit8.falchion.JvmProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * @author kawasima
 */
public class JstatMonitor implements GcMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(JstatMonitor.class);

    private Process process;
    private GcStat stat;
    private Thread monitorThread;

    @Override
    public void start(JvmProcess jvmProcess) {
        try {
            process = new ProcessBuilder("jstat", "-gcutil",
                    Long.toString(jvmProcess.getPid()), "3000")
                    .start();
            JstatGcutilParser parser = new JstatGcutilParser();
            monitorThread = new Thread(() -> {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                try {

                    String line = reader.readLine();
                    while ((line = reader.readLine()) != null) {
                        stat = parser.parse(line);
                    }
                } catch (IOException ex) {
                    LOG.error("Failed to read jstat output", ex);
                }
            });
            monitorThread.start();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void stop() {
        if (process == null) return;

        try {
            process.getOutputStream().close();
            process.getErrorStream().close();
            process.getInputStream().close();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        } finally {
            process.destroy();
        }
        LOG.info("JstatMonitor stop: {}", stat);
    }

    @Override
    public GcStat getStat() {
        return stat;
    }
}

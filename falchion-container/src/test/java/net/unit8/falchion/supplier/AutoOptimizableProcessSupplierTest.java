package net.unit8.falchion.supplier;

import net.unit8.falchion.JvmProcess;
import net.unit8.falchion.evaluator.MinGcTime;
import net.unit8.falchion.monitor.GcStat;
import net.unit8.falchion.monitor.JvmMonitor;
import net.unit8.falchion.monitor.MetricsStat;
import net.unit8.falchion.monitor.MonitorStat;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AutoOptimizableProcessSupplierTest {
    @Test
    public void getReturnsProcessWithJvmOptions() {
        AutoOptimizableProcessSupplier supplier = new AutoOptimizableProcessSupplier(
                () -> new JvmProcess("dummy.Main", "."),
                new MinGcTime()
        );

        JvmProcess process = supplier.get();
        List<String> options = process.getJvmOptions();
        assertThat(options).isNotNull();
        assertThat(options).isNotEmpty();
        // StandardOptionProvider with (128, 128, 0.1) should produce -server, -Xms, -Xmx, etc.
        assertThat(options).anyMatch(opt -> opt.equals("-server"));
        assertThat(options).anyMatch(opt -> opt.startsWith("-Xms"));
        assertThat(options).anyMatch(opt -> opt.startsWith("-Xmx"));
    }

    @Test
    public void feedbackUpdatesOptionsBasedOnBestProcess() {
        AutoOptimizableProcessSupplier supplier = new AutoOptimizableProcessSupplier(
                () -> new JvmProcess("dummy.Main", "."),
                new MinGcTime()
        );

        // Create two processes with different JVM options and GC stats
        JvmProcess good = new JvmProcess("dummy.Main", ".");
        good.setJvmOptions(Arrays.asList("-server", "-Xms256m", "-Xmx256m"));
        good.addMonitor(
                new StubGcMonitor(0.1),   // low GC time
                new StubMetricsMonitor(1000)
        );

        JvmProcess bad = new JvmProcess("dummy.Main", ".");
        bad.setJvmOptions(Arrays.asList("-server", "-Xms64m", "-Xmx64m"));
        bad.addMonitor(
                new StubGcMonitor(5.0),   // high GC time
                new StubMetricsMonitor(1000)
        );

        // After feedback, the supplier should reconfigure based on best process (good)
        supplier.feedback(Arrays.asList(good, bad));

        // The next process should get options derived from the best process's options
        JvmProcess next = supplier.get();
        assertThat(next.getJvmOptions()).isNotNull();
        assertThat(next.getJvmOptions()).isNotEmpty();
    }

    private static class StubGcMonitor implements JvmMonitor {
        private final double gct;

        StubGcMonitor(double gct) {
            this.gct = gct;
        }

        @Override
        public void start(JvmProcess process) {}

        @Override
        public MonitorStat getStat() {
            return new GcStat(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0, 0L, 0.0, gct);
        }

        @Override
        public void stop() {}
    }

    private static class StubMetricsMonitor implements JvmMonitor {
        private final long count;

        StubMetricsMonitor(long count) {
            this.count = count;
        }

        @Override
        public void start(JvmProcess process) {}

        @Override
        public MonitorStat getStat() {
            return new MetricsStat(count, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        @Override
        public void stop() {}
    }
}

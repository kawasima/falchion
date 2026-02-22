package net.unit8.falchion.evaluator;

import net.unit8.falchion.JvmProcess;
import net.unit8.falchion.monitor.GcStat;
import net.unit8.falchion.monitor.JvmMonitor;
import net.unit8.falchion.monitor.MetricsStat;
import net.unit8.falchion.monitor.MonitorStat;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class MinFullGcCountTest {
    private JvmProcess createProcessWithStats(double fgct, long metricsCount) {
        JvmProcess process = new JvmProcess("dummy.Main", ".");
        process.addMonitor(new StubGcMonitor(fgct), new StubMetricsMonitor(metricsCount));
        return process;
    }

    @Test
    public void selectsProcessWithLowestFullGcTimePerRequest() {
        MinFullGcCount evaluator = new MinFullGcCount();

        // fgct/count: process1=2.0/100=0.02, process2=0.3/100=0.003, process3=1.0/100=0.01
        JvmProcess process1 = createProcessWithStats(2.0, 100);
        JvmProcess process2 = createProcessWithStats(0.3, 100);
        JvmProcess process3 = createProcessWithStats(1.0, 100);

        JvmProcess best = evaluator.evaluate(Arrays.asList(process1, process2, process3));
        assertThat(best).isSameAs(process2);
    }

    @Test
    public void returnsNullForEmptyCollection() {
        MinFullGcCount evaluator = new MinFullGcCount();
        JvmProcess result = evaluator.evaluate(Collections.emptyList());
        assertThat(result).isNull();
    }

    @Test
    public void singleProcessIsReturnedAsIs() {
        MinFullGcCount evaluator = new MinFullGcCount();

        JvmProcess process = createProcessWithStats(1.5, 200);
        JvmProcess best = evaluator.evaluate(Collections.singletonList(process));
        assertThat(best).isSameAs(process);
    }

    @Test
    public void zeroMetricsCountResultsInZeroScore() {
        MinFullGcCount evaluator = new MinFullGcCount();

        JvmProcess processZeroCount = createProcessWithStats(1.0, 0);
        JvmProcess processNormal = createProcessWithStats(0.5, 100);

        JvmProcess best = evaluator.evaluate(Arrays.asList(processZeroCount, processNormal));
        assertThat(best).isSameAs(processZeroCount);
    }

    private static class StubGcMonitor implements JvmMonitor {
        private final double fgct;

        StubGcMonitor(double fgct) {
            this.fgct = fgct;
        }

        @Override
        public void start(JvmProcess process) {}

        @Override
        public MonitorStat getStat() {
            return new GcStat(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0, 0L, fgct, 0.0);
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

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

public class MinGcTimeTest {
    private JvmProcess createProcessWithStats(double gct, long metricsCount) {
        JvmProcess process = new JvmProcess("dummy.Main", ".");
        process.addMonitor(new StubGcMonitor(gct), new StubMetricsMonitor(metricsCount));
        return process;
    }

    @Test
    public void selectsProcessWithLowestGcTimePerRequest() {
        MinGcTime evaluator = new MinGcTime();

        // gct/count: process1=1.0/100=0.01, process2=0.5/100=0.005, process3=2.0/100=0.02
        JvmProcess process1 = createProcessWithStats(1.0, 100);
        JvmProcess process2 = createProcessWithStats(0.5, 100);
        JvmProcess process3 = createProcessWithStats(2.0, 100);

        JvmProcess best = evaluator.evaluate(Arrays.asList(process1, process2, process3));
        assertThat(best).isSameAs(process2);
    }

    @Test
    public void returnsNullForEmptyCollection() {
        MinGcTime evaluator = new MinGcTime();
        JvmProcess result = evaluator.evaluate(Collections.emptyList());
        assertThat(result).isNull();
    }

    @Test
    public void zeroMetricsCountResultsInZeroScore() {
        MinGcTime evaluator = new MinGcTime();

        JvmProcess processZeroCount = createProcessWithStats(1.0, 0);
        JvmProcess processNormal = createProcessWithStats(0.5, 100);

        // score: processZeroCount=0, processNormal=0.005
        // processZeroCount should be selected because -1 < 0 < 0.005... no,
        // score is 0 for zero count (not -1). 0 < 0.005, so processZeroCount is best.
        JvmProcess best = evaluator.evaluate(Arrays.asList(processZeroCount, processNormal));
        assertThat(best).isSameAs(processZeroCount);
    }

    @Test
    public void processWithNoStatsGetsNegativeScore() {
        MinGcTime evaluator = new MinGcTime();

        // Process with no monitors => score returns -1
        JvmProcess processNoStats = new JvmProcess("dummy.Main", ".");
        JvmProcess processNormal = createProcessWithStats(0.5, 100);

        // score: processNoStats=-1, processNormal=0.005
        // -1 < 0.005, so processNoStats is selected
        JvmProcess best = evaluator.evaluate(Arrays.asList(processNoStats, processNormal));
        assertThat(best).isSameAs(processNoStats);
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

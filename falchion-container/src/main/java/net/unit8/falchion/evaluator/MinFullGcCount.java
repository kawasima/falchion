package net.unit8.falchion.evaluator;

import net.unit8.falchion.JvmProcess;
import net.unit8.falchion.monitor.GcStat;
import net.unit8.falchion.monitor.MetricsStat;
import net.unit8.falchion.monitor.MonitorStat;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * @author kawasima
 */
public class MinFullGcCount implements Evaluator {
    private double score(List<MonitorStat> stats) {
        Optional<GcStat> gcStat = stats.stream()
                .filter(GcStat.class::isInstance)
                .map(GcStat.class::cast)
                .findFirst();

        Optional<MetricsStat> metricsStat = stats.stream()
                .filter(MetricsStat.class::isInstance)
                .map(MetricsStat.class::cast)
                .findFirst();

        if (!gcStat.isPresent() || !metricsStat.isPresent()) {
            return -1;
        }

        if (metricsStat.get().getCount() == 0) {
            return 0;
        }

        return gcStat.get().getGct() / metricsStat.get().getCount();
    }

    public JvmProcess evaluate(Collection<JvmProcess> processes) {
        return processes.stream()
                .sorted(Comparator.comparing(p -> score(p.getMonitorStats())))
                .findFirst()
                .orElse(null);
    }
}

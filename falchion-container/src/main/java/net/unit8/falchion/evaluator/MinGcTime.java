package net.unit8.falchion.evaluator;

import net.unit8.falchion.JvmProcess;
import net.unit8.falchion.monitor.GcStat;
import net.unit8.falchion.monitor.MetricsStat;
import net.unit8.falchion.monitor.MonitorStat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * @author kawasima
 */
public class MinGcTime implements Evaluator {
    private static final Logger LOG = LoggerFactory.getLogger(MinGcTime.class);

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
        processes.forEach(p -> {
            double s = score(p.getMonitorStats());
            LOG.info("  process id={}, jvmOptions={}, score(GCT/requests)={}", p.getId(), p.getJvmOptions(), s);
        });
        return processes.stream().min(Comparator.comparing(p -> score(p.getMonitorStats())))
                .orElse(null);
    }
}

package net.unit8.falchion.monitor;

import java.util.function.Supplier;

/**
 * @author kawasima
 */
public enum MonitorSupplier {
    GCUTIL_JSTAT(JstatMonitor::new),
    METRICS_JMX(MetricsJmxMonitor::new);

    private Supplier<JvmMonitor> monitorSupplier;

    MonitorSupplier(Supplier<JvmMonitor> monitorSupplier) {
        this.monitorSupplier = monitorSupplier;
    }

    public JvmMonitor createMonitor() {
        return monitorSupplier.get();
    }
}

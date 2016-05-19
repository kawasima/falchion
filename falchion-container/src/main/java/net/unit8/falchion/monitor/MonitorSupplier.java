package net.unit8.falchion.monitor;

import java.util.function.Supplier;

/**
 * @author kawasima
 */
public enum MonitorSupplier {
    GCUTIL_JSTAT(() -> new JstatMonitor()),
    METRICS_JMX(() -> new MetricsJmxMonitor());

    private Supplier<JvmMonitor> monitorSupplier;

    MonitorSupplier(Supplier<JvmMonitor> monitorSupplier) {
        this.monitorSupplier = monitorSupplier;
    }

    public JvmMonitor createMonitor() {
        return monitorSupplier.get();
    }
}

package net.unit8.falchion.monitor;

import net.unit8.falchion.JvmProcess;

/**
 * @author kawasima
 */
public interface JvmMonitor {
    void start(JvmProcess process);
    MonitorStat getStat();
    void stop();
}

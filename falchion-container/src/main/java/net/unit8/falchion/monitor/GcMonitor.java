package net.unit8.falchion.monitor;

/**
 * @author kawasima
 */
public interface GcMonitor extends JvmMonitor {
    GcStat getStat();
}

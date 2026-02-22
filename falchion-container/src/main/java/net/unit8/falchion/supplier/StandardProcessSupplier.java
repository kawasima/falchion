package net.unit8.falchion.supplier;

import net.unit8.falchion.JvmProcess;
import net.unit8.falchion.monitor.MonitorSupplier;
import net.unit8.falchion.option.GcAlgorithm;
import net.unit8.falchion.option.provider.StandardOptionProvider;

import java.io.File;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author kawasima
 */
public class StandardProcessSupplier implements Supplier<JvmProcess> {
    private String javaOpts;
    private String mainClass;
    private String classpath;
    private File logDir;
    private Set<MonitorSupplier> monitorSuppliers;
    private StandardOptionProvider standardOptionProvider;

    public StandardProcessSupplier(String mainClass, String classpath, String javaOpts, File logDir, Set<MonitorSupplier> monitorSuppliers) {
        this.javaOpts = javaOpts;
        this.mainClass = mainClass;
        this.classpath = classpath;
        this.logDir = logDir;
        this.monitorSuppliers = monitorSuppliers;
        standardOptionProvider = new StandardOptionProvider(javaOpts, 0.0);
    }

    public void setGcAlgorithm(GcAlgorithm gcAlgorithm) {
        standardOptionProvider.setGcAlgorithm(gcAlgorithm);
    }

    @Override
    public JvmProcess get() {
        JvmProcess p = new JvmProcess(mainClass, classpath);
        p.setJvmOptions(standardOptionProvider.getOptions());
        if (logDir != null && logDir.isDirectory()) {
            p.setIoDir(logDir);
        }
        monitorSuppliers.forEach(ms -> p.addMonitor(ms.createMonitor()));
        return p;
    }
}

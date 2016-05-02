package net.unit8.falchion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.*;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author kawasima
 */
public class Container {
    private static final Logger LOG = LoggerFactory.getLogger(Container.class);
    private int poolSize;
    private JvmPool pool;

    public Container(int poolSize) {
        this.poolSize = poolSize;
    }

    private String getClasspath() {
        LOG.info(System.getProperty("java.class.path"));
        return Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .map(File::new)
                .map(File::getAbsolutePath)
                .collect(Collectors.joining(":"));
    }

    public void start(final String mainClass, String classpath) {
        pool = new JvmPool(poolSize, () -> new JvmProcess(mainClass, classpath));
        pool.fill();
    }

    public void start(final String mainClass) {
        start(mainClass, getClasspath());
    }

    public JvmPool getPool() {
        return pool;
    }
}

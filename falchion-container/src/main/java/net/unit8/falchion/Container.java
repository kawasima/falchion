package net.unit8.falchion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author kawasima
 */
public class Container {
    private static final Logger LOG = LoggerFactory.getLogger(Container.class);
    private int poolSize;
    private JvmPool pool;
    private static final Signal HUP  = new Signal("HUP");
    private static final Signal TERM = new Signal("TERM");
    private static final Signal USR1 = new Signal("USR1");

    public Container(int poolSize) {
        this.poolSize = poolSize;
        Signal.handle(TERM, signal -> pool.shutdown());
        Signal.handle(HUP, signal -> pool.refresh());
        Signal.handle(USR1, signal -> pool.info());
    }

    private URI toURI(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getClasspath() {
        return Arrays.stream(URLClassLoader.class.cast(getClass().getClassLoader()).getURLs())
                .filter(url -> url.getProtocol().equals("file"))
                .map(this::toURI)
                .map(File::new)
                .map(File::getAbsolutePath)
                .collect(Collectors.joining(":"));
    }

    public void start(final String mainClass) {
        String classpath = getClasspath();
        pool = new JvmPool(poolSize, () -> new JvmProcess(mainClass, classpath));
        pool.create();
    }

    public JvmPool getPool() {
        return pool;
    }

    public static void main(String[] args) {
        Container container = new Container(1);
        ApiServer apiServer = new ApiServer(container);
        apiServer.start();
        container.start(args[0]);
    }
}

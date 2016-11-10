package net.unit8.falchion.api;

import com.sun.net.httpserver.HttpExchange;
import net.unit8.falchion.Container;
import net.unit8.falchion.JvmPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author kawasima
 */
public class RefreshContainerHandler extends AbstractApi {
    private static final Logger LOG = LoggerFactory.getLogger(RefreshContainerHandler.class);
    private static final Pattern RE_ID = Pattern.compile("/refresh/([a-zA-Z0-9.-]+)$");

    public RefreshContainerHandler(Container container) {
        super(container);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Matcher m = RE_ID.matcher(exchange.getRequestURI().getPath());
        Container container = getContainer();
        JvmPool pool = container.getPool();
        if (m.find()) {
            String version = m.group(1);
            String classpath = container.createClasspath(container.getBasedir(), version);
            if (Objects.equals(classpath, container.getBasedir())) {
                sendBadRequest(exchange, exchange.getRequestURI().getPath());
                return;
            }
            pool.setClasspath(classpath);
            LOG.info("The version of the application has been changed. New version is '{}'", version);
        }
        pool.refresh();
        sendNoContent(exchange);
    }
}

package net.unit8.falchion.api;

import com.sun.net.httpserver.HttpExchange;
import net.unit8.falchion.Container;
import net.unit8.falchion.JvmProcess;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author kawasima
 */
public class ReadyJvmHandler extends AbstractApi {
    private static final Pattern RE_ID = Pattern.compile("/([a-zA-Z0-9]+)/ready$");

    public ReadyJvmHandler(Container container) {
        super(container);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Matcher m = RE_ID.matcher(exchange.getRequestURI().getPath());
        try {
            if (m.find()) {
                String id = m.group(1);
                JvmProcess process = getContainer()
                        .getPool()
                        .getProcessByPid(Long.parseLong(id));
                process.ready();

                sendNoContent(exchange);
            } else {
                sendBadRequest(exchange, exchange.getRequestURI().getPath());
            }
        } catch (Exception ex) {
            sendBadRequest(exchange, exchange.getRequestURI().getPath());
        }
    }
}

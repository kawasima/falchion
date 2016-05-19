package net.unit8.falchion.api;

import com.sun.net.httpserver.HttpExchange;
import net.unit8.falchion.Container;
import net.unit8.falchion.JvmProcess;
import net.unit8.falchion.monitor.MonitorStat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import java.io.IOException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author kawasima
 */
public class ShowJvmHandler extends AbstractApi {
    private static final Logger LOG = LoggerFactory.getLogger(ShowJvmHandler.class);
    private static final Pattern RE_ID = Pattern.compile("/([a-zA-Z0-9]+)$");

    public ShowJvmHandler(Container container) {
        super(container);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Matcher m = RE_ID.matcher(exchange.getRequestURI().getPath());

        try {
            if (m.find()) {
                String id = m.group(1);
                JsonObjectBuilder jvmStatus = Json.createObjectBuilder();
                JsonArrayBuilder monitorStats = Json.createArrayBuilder();
                JvmProcess process = getContainer()
                        .getPool()
                        .getProcess(id);

                process.getMonitorStats()
                        .stream()
                        .map(MonitorStat::toJson)
                        .filter(Objects::nonNull)
                        .forEach(json -> monitorStats.add(json));

                jvmStatus.add("id", process.getId());
                jvmStatus.add("pid", process.getPid());
                jvmStatus.add("uptime", process.getUptime());
                jvmStatus.add("options", process.getJvmOptions().stream()
                        .collect(Collectors.joining(" ")));
                jvmStatus.add("stats", monitorStats.build());
                sendJson(exchange, jvmStatus.build());
            } else {
                sendBadRequest(exchange, exchange.getRequestURI().getPath());
            }
        } catch (Exception ex) {
            LOG.error("", ex);
            sendBadRequest(exchange, exchange.getRequestURI().getPath());
        }
    }
}

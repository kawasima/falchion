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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Returns aggregated metrics for all active JVM processes.
 *
 * @author kawasima
 */
public class MetricsHandler extends AbstractApi {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsHandler.class);

    public MetricsHandler(Container container) {
        super(container);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            List<JvmProcess> processes = getContainer().getPool().getActiveProcesses();

            JsonArrayBuilder processesArray = Json.createArrayBuilder();
            for (JvmProcess process : processes) {
                JsonObjectBuilder processObj = Json.createObjectBuilder();
                processObj.add("id", process.getId());
                processObj.add("pid", process.getPid());
                processObj.add("uptime", process.getUptime());

                List<String> jvmOptions = process.getJvmOptions();
                if (jvmOptions != null) {
                    processObj.add("options", jvmOptions.stream().collect(Collectors.joining(" ")));
                }

                JsonArrayBuilder statsArray = Json.createArrayBuilder();
                process.getMonitorStats().stream()
                        .filter(Objects::nonNull)
                        .map(MonitorStat::toJson)
                        .forEach(statsArray::add);
                processObj.add("stats", statsArray.build());

                processesArray.add(processObj.build());
            }

            JsonObjectBuilder result = Json.createObjectBuilder();
            result.add("poolSize", processes.size());
            result.add("activeProcesses", processes.size());
            result.add("processes", processesArray.build());

            sendJson(exchange, result.build());
        } catch (Exception ex) {
            LOG.error("Failed to collect metrics", ex);
            sendBadRequest(exchange, "Failed to collect metrics");
        }
    }
}

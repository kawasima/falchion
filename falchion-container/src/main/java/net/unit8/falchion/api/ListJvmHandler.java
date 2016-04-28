package net.unit8.falchion.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.unit8.falchion.Container;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author kawasima
 */
public class ListJvmHandler extends AbstractApi {
    public ListJvmHandler(Container container) {
        super(container);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        getContainer().getPool().getActiveProcesses().stream().forEach(p ->
                arrayBuilder.add(Json.createObjectBuilder()
                        .add("id", p.getId())
                        .add("pid", p.getPid())));

        sendJson(exchange, arrayBuilder.build());
    }
}

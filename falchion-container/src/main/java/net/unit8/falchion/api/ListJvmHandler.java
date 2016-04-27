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
public class ListJvmHandler implements HttpHandler {
    private Container container;

    public ListJvmHandler(Container container) {
        this.container = container;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        container.getPool().getActiveProcesses().stream().forEach(p ->
                arrayBuilder.add(Json.createObjectBuilder()
                        .add("id", p.getId())
                        .add("pid", p.getPid())));

        String body = arrayBuilder.build().toString();
        httpExchange.sendResponseHeaders(200, body.length());
        httpExchange.getResponseBody().write(body.getBytes(StandardCharsets.ISO_8859_1));
    }
}

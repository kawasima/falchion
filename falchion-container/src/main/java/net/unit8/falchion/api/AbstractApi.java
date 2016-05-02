package net.unit8.falchion.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.unit8.falchion.Container;

import javax.json.Json;
import javax.json.JsonValue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author kawasima
 */
public abstract class AbstractApi implements HttpHandler {
    private final Container container;

    public AbstractApi(Container container) {
        this.container = container;
    }

    public Container getContainer() {
        return container;
    }

    protected void sendJson(HttpExchange exchange, JsonValue jsonValue) throws IOException {
        String body = jsonValue.toString();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length());
        exchange.getResponseBody().write(body.getBytes(StandardCharsets.ISO_8859_1));
    }

    protected void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
    }

    protected void sendBadRequest(HttpExchange exchange, String message) throws IOException {
        String body = Json.createObjectBuilder().add("message", message).build().toString();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(400, body.length());
        exchange.getResponseBody().write(body.getBytes(StandardCharsets.ISO_8859_1));
    }

}

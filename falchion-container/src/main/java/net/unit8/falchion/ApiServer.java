package net.unit8.falchion;

import com.sun.net.httpserver.HttpServer;
import net.unit8.falchion.api.ListJvmHandler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;

/**
 * @author kawasima
 */
public class ApiServer {
    final HttpServer httpServer;
    public ApiServer(Container container) {
        ListJvmHandler listJvmHandler = new ListJvmHandler(container);

        try {
            httpServer = HttpServer.create(new InetSocketAddress(44010), 1);
            httpServer.createContext("/", exchange -> {
                try {
                    String path = exchange.getRequestURI().getPath();
                    if (path.matches("/jvms")) {
                        listJvmHandler.handle(exchange);
                    } else {
                        exchange.sendResponseHeaders(404, -1);
                        exchange.getResponseBody().write("Not Found".getBytes());
                    }
                } finally {
                    exchange.close();
                }
            });
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }


    public void start() {
        httpServer.start();
    }

    public void stop() {
        httpServer.stop(0);
    }
}

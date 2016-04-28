package net.unit8.falchion;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import me.geso.routes.RoutingResult;
import me.geso.routes.WebRouter;
import net.unit8.falchion.api.AbstractApi;
import net.unit8.falchion.api.ListJvmHandler;
import net.unit8.falchion.api.ReadyJvmHandler;
import net.unit8.falchion.api.RefreshContainerHandler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.Locale;

/**
 * @author kawasima
 */
public class ApiServer {
    final HttpServer httpServer;
    public ApiServer(Container container) {
        ListJvmHandler listJvmHandler = new ListJvmHandler(container);
        RefreshContainerHandler refreshContainerHandler = new RefreshContainerHandler(container);
        ReadyJvmHandler readyJvmHandler = new ReadyJvmHandler(container);

        WebRouter<HttpHandler> router = new WebRouter<>();
        router.get("/jvms", listJvmHandler::handle);
        router.post("/jvm/{pid}", readyJvmHandler::handle);
        router.post("/container/refresh", refreshContainerHandler::handle);

        try {
            httpServer = HttpServer.create(new InetSocketAddress(44010), 1);
            httpServer.createContext("/", exchange -> {
                try {
                    RoutingResult<HttpHandler> rr = router.match(
                            exchange.getRequestMethod().toUpperCase(Locale.US),
                            exchange.getRequestURI().getPath());
                    if (rr != null) {
                        if (rr.methodAllowed()) {
                            HttpHandler handler = rr.getDestination();
                            handler.handle(exchange);
                        } else {
                            byte[] body = "Method Not Allowed".getBytes();
                            exchange.sendResponseHeaders(405, body.length);
                            exchange.getResponseBody().write(body);
                        }
                    } else {
                        byte[] body = "Not Found".getBytes();
                        exchange.sendResponseHeaders(404, body.length);
                        exchange.getResponseBody().write(body);
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

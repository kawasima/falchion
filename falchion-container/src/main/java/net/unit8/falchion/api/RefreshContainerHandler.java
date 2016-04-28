package net.unit8.falchion.api;

import com.sun.net.httpserver.HttpExchange;
import net.unit8.falchion.Container;

import java.io.IOException;

/**
 * @author kawasima
 */
public class RefreshContainerHandler extends AbstractApi {
    public RefreshContainerHandler(Container container) {
        super(container);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        getContainer().getPool().refresh();
        sendNoContent(exchange);
    }
}

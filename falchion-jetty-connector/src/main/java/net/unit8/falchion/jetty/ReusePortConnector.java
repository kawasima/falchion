package net.unit8.falchion.jetty;

import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

/**
 * A ServerConnector with SO_REUSEPORT enabled, allowing multiple
 * processes to bind to the same port for zero-downtime restarts.
 *
 * In Jetty 12, SO_REUSEPORT is natively supported via
 * {@link ServerConnector#setReusePort(boolean)}, so this class
 * simply pre-configures that setting.
 *
 * @author kawasima
 */
public class ReusePortConnector extends ServerConnector {
    public ReusePortConnector(Server server) {
        super(server);
        setReusePort(true);
    }

    public ReusePortConnector(Server server, ConnectionFactory... factories) {
        super(server, factories);
        setReusePort(true);
    }
}

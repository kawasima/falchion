package net.unit8.falchion.example.springboot;

import net.unit8.falchion.jetty9.ReusePortConnector;
import org.eclipse.jetty.server.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/**
 * Replaces Jetty's default ServerConnector with ReusePortConnector,
 * enabling SO_REUSEPORT so that multiple falchion child processes
 * can bind to the same port.
 */
@Component
public class ReusePortJettyCustomizer implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {

    @Value("${server.port:3000}")
    private int port;

    @Override
    public void customize(JettyServletWebServerFactory factory) {
        factory.addServerCustomizers((JettyServerCustomizer) server -> {
            // Remove all default connectors
            java.util.Arrays.stream(server.getConnectors())
                    .forEach(server::removeConnector);

            // Add ReusePortConnector
            ReusePortConnector connector = new ReusePortConnector(server);
            connector.setPort(port);
            server.addConnector(connector);
        });
    }
}

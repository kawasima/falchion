package net.unit8.falchion.example.jetty9;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jetty9.InstrumentedHandler;
import net.unit8.falchion.jetty9.ReusePortConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.util.annotation.Name;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.ServerSocketChannel;
import java.util.Random;
import java.util.UUID;

/**
 * @author kawasima
 */
public class Main {
    public static void main(String[] args) throws Exception {
        MetricRegistry metrics = new MetricRegistry();
        String serverId = UUID.randomUUID().toString();
        Server server = new Server();
        ReusePortConnector connector = new ReusePortConnector(server);
        connector.setPort(3000);

        HandlerWrapper handler = new InstrumentedHandler(metrics, "falchion");
        handler.setHandler(new AbstractHandler() {
            private static final String CHARS = "0123456789ABCDEF";
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                String s = new Random().ints(0, CHARS.length())
                        .mapToObj(i -> CHARS.charAt(i))
                        .limit(4096)
                        .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                        .toString();
                response.setStatus(200);
                PrintWriter out = response.getWriter();
                out.println("hello " + serverId);
                out.println("secret " + s);
                baseRequest.setHandled(true);
            }
        });

        server.setHandler(handler);
        server.setStopAtShutdown(true);
        server.setStopTimeout(3000);
        server.addConnector(connector);
        server.start();

        final JmxReporter reporter = JmxReporter.forRegistry(metrics).build();
        reporter.start();

        String vmName = ManagementFactory.getRuntimeMXBean().getName();
        long pid = Long.valueOf(vmName.split("@")[0]);
        HttpResponse response = HttpRequest
                .create(new URI("http://localhost:44010/jvm/" + pid + "/ready"))
                .POST()
                .response();
        response.body(HttpResponse.ignoreBody());
        int status = response.statusCode();
        if (status != 204) {
            server.stop();
        }

        server.join();
    }

}

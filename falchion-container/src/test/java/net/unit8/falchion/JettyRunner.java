package net.unit8.falchion;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.annotation.Name;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.ServerSocketChannel;
import java.util.UUID;

/**
 * @author kawasima
 */
public class JettyRunner {
    static class ReusePortAvailableConnector extends ServerConnector {
        public ReusePortAvailableConnector(@Name("server") Server server) {
            super(server);
        }

        @Override
        public void open() throws IOException {
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.setOption(StandardSocketOptions.SO_REUSEPORT, true);

            InetSocketAddress bindAddress = getHost() == null ? new InetSocketAddress(getPort()) : new InetSocketAddress(getHost(), getPort());
            serverChannel.socket().setReuseAddress(getReuseAddress());
            serverChannel.socket().bind(bindAddress, getAcceptQueueSize());
            serverChannel.configureBlocking(true);
            addBean(serverChannel);
            try {
                Field acceptChannel = ServerConnector.class.getDeclaredField("_acceptChannel");
                acceptChannel.setAccessible(true);
                acceptChannel.set(this, serverChannel);
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String serverId = UUID.randomUUID().toString();
        Server server = new Server();
        ReusePortAvailableConnector connector = new ReusePortAvailableConnector(server);
        connector.setPort(3000);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                response.setStatus(200);
                PrintWriter out = response.getWriter();
                out.println("hello " + serverId);
                baseRequest.setHandled(true);
            }
        });
        server.setStopAtShutdown(true);
        server.setStopTimeout(3000);
        server.addConnector(connector);
        server.start();

        String vmName= ManagementFactory.getRuntimeMXBean().getName();
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

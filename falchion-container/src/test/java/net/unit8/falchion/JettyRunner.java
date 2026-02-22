package net.unit8.falchion;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.UUID;

/**
 * @author kawasima
 */
public class JettyRunner {

    public static void main(String[] args) throws Exception {
        String serverId = UUID.randomUUID().toString();
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(3000);
        connector.setReusePort(true);
        server.setHandler(new Handler.Abstract() {
            private static final String CHARS = "0123456789ABCDEF";
            @Override
            public boolean handle(Request request, Response response, Callback callback) throws Exception {
                String s = new Random().ints(0, CHARS.length())
                        .mapToObj(CHARS::charAt)
                        .limit(4096)
                        .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                        .toString();
                response.setStatus(200);
                String body = "hello " + serverId + "\nsecret " + s + "\n";
                response.write(true, ByteBuffer.wrap(body.getBytes(StandardCharsets.UTF_8)), callback);
                return true;
            }
        });
        server.setStopAtShutdown(true);
        server.setStopTimeout(3000);
        server.addConnector(connector);
        server.start();

        long pid = ProcessHandle.current().pid();

        server.join();
    }
}

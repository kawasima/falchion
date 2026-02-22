package net.unit8.falchion.example.jetty;

import com.codahale.metrics.jmx.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import net.unit8.falchion.jetty.ReusePortConnector;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Callback;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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

        AllStringCache cache = new AllStringCache();

        server.setHandler(new Handler.Abstract() {
            private static final String CHARS = "0123456789ABCDEF";
            @Override
            public boolean handle(Request request, Response response, Callback callback) throws Exception {
                String s = new Random().ints(0, CHARS.length())
                        .mapToObj(CHARS::charAt)
                        .limit(4096)
                        .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                        .toString();
                cache.put(s);
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

        final JmxReporter reporter = JmxReporter.forRegistry(metrics).build();
        reporter.start();

        long pid = ProcessHandle.current().pid();

        OkHttpClient client = new OkHttpClient();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("http://localhost:44010/jvm/" + pid + "/ready")
                .post(RequestBody.create(MediaType.parse("application/json"), ""))
                .build();

        client.newCall(request)
                .enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        try {
                            server.stop();
                        } catch (Exception stopEx) {
                            throw new IllegalStateException("Fail to stop a server", stopEx);
                        }
                    }

                    @Override
                    public void onResponse(Call call, okhttp3.Response response) throws IOException {
                        if (response.code() != 204) {
                            try {
                                server.stop();
                            } catch (Exception e) {
                                throw new IllegalStateException("Fail to stop a server", e);
                            }
                        }
                    }
                });

        server.join();
    }

}

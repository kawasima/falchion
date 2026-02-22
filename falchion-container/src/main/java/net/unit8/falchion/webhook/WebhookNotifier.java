package net.unit8.falchion.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sends lifecycle event notifications to a webhook URL via HTTP POST (fire-and-forget).
 *
 * @author kawasima
 */
public class WebhookNotifier {
    private static final Logger LOG = LoggerFactory.getLogger(WebhookNotifier.class);

    private final String webhookUrl;
    private final ExecutorService executor;

    public WebhookNotifier(String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "webhook-notifier");
            t.setDaemon(true);
            return t;
        });
        LOG.info("Webhook notifier configured: url={}", webhookUrl);
    }

    public void notifyProcessStarted(String id, long pid) {
        send(Json.createObjectBuilder()
                .add("event", "process_started")
                .add("id", id)
                .add("pid", pid));
    }

    public void notifyProcessStopped(String id, long pid) {
        send(Json.createObjectBuilder()
                .add("event", "process_stopped")
                .add("id", id)
                .add("pid", pid));
    }

    public void notifyRefreshStarted() {
        send(Json.createObjectBuilder()
                .add("event", "refresh_started"));
    }

    public void notifyRefreshCompleted() {
        send(Json.createObjectBuilder()
                .add("event", "refresh_completed"));
    }

    public void notifyHealthCheckFailed(String id, long pid, int failures) {
        send(Json.createObjectBuilder()
                .add("event", "health_check_failed")
                .add("id", id)
                .add("pid", pid)
                .add("failures", failures));
    }

    private void send(JsonObjectBuilder builder) {
        builder.add("timestamp", Instant.now().toString());
        String payload = builder.build().toString();
        executor.submit(() -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.getBytes(StandardCharsets.UTF_8));
                }
                int responseCode = conn.getResponseCode();
                if (responseCode >= 400) {
                    LOG.warn("Webhook returned status {}", responseCode);
                }
                conn.disconnect();
            } catch (IOException e) {
                LOG.debug("Webhook notification failed: {}", e.getMessage());
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}

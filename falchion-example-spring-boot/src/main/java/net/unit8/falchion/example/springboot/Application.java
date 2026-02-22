package net.unit8.falchion.example.springboot;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.lang.management.ManagementFactory;

@SpringBootApplication
public class Application implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    @Bean
    public MetricRegistry metricRegistry() {
        return new MetricRegistry();
    }

    @Bean
    public JmxReporter jmxReporter(MetricRegistry metricRegistry) {
        JmxReporter reporter = JmxReporter.forRegistry(metricRegistry).build();
        reporter.start();
        return reporter;
    }

    @Override
    public void run(ApplicationArguments args) {
        String vmName = ManagementFactory.getRuntimeMXBean().getName();
        long pid = Long.parseLong(vmName.split("@")[0]);
        LOG.info("Application started with pid={}", pid);

        OkHttpClient client = new OkHttpClient();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("http://localhost:44010/jvm/" + pid + "/ready")
                .post(RequestBody.create(MediaType.parse("application/json"), ""))
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LOG.warn("Failed to notify readiness to falchion container. Running in standalone mode.", e);
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                try (response) {
                    if (response.code() == 204) {
                        LOG.info("Successfully notified readiness to falchion container");
                    } else {
                        LOG.warn("Unexpected response from falchion container: {}", response.code());
                    }
                }
            }
        });
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

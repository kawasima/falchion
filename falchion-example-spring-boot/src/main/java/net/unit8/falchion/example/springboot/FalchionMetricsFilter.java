package net.unit8.falchion.example.springboot;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import java.io.IOException;

/**
 * Servlet filter that records request metrics using Dropwizard's MetricRegistry.
 *
 * The timer is registered as "falchion.requests" to be compatible with
 * MetricsJmxMonitor in falchion-container, which reads the MBean
 * "metrics:name=falchion.requests" via JMX.
 */
@Component
public class FalchionMetricsFilter implements Filter {
    private final Timer requestTimer;

    public FalchionMetricsFilter(MetricRegistry metricRegistry) {
        this.requestTimer = metricRegistry.timer("falchion.requests");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        final Timer.Context context = requestTimer.time();
        try {
            chain.doFilter(request, response);
        } finally {
            context.stop();
        }
    }
}

package net.unit8.falchion.monitor;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import net.unit8.falchion.JvmProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;

/**
 * @author kawasima
 */
public class MetricsJmxMonitor implements JvmMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsJmxMonitor.class);
    private MBeanServerConnection conn;
    private JMXConnector connector;
    private ObjectName requestsMBean;

    private MBeanServerConnection getLocalMBeanServerConnection(int pid) {
        try {
            LOG.info("pid={}", pid);
            VirtualMachine vm = VirtualMachine.attach(Integer.toString(pid));

            String address = vm.startLocalManagementAgent();
            JMXServiceURL jmxUrl = new JMXServiceURL(address);
            connector = JMXConnectorFactory.connect(jmxUrl);
            return connector.getMBeanServerConnection();
        } catch (IOException | AttachNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
    }

    private ObjectName objectNameFromString(String name) {
        try {
            return new ObjectName(name);
        } catch (MalformedObjectNameException ex) {
            throw new IllegalArgumentException(name, ex);
        }
    }

    @Override
    public void start(JvmProcess process) {
        conn = getLocalMBeanServerConnection((int) process.getPid());
        LOG.info("conn={}", conn);
        requestsMBean = objectNameFromString("metrics:name=falchion.requests");
    }

    @Override
    public MonitorStat getStat() {
        try {
            if (conn.isRegistered(requestsMBean)) {
                return new MetricsStat((long) conn.getAttribute(requestsMBean, "Count"),
                        (double) conn.getAttribute(requestsMBean, "Mean"),
                        (double) conn.getAttribute(requestsMBean, "MeanRate"),
                        (double) conn.getAttribute(requestsMBean, "Min"),
                        (double) conn.getAttribute(requestsMBean, "Max"),
                        (double) conn.getAttribute(requestsMBean, "StdDev"),
                        (double) conn.getAttribute(requestsMBean, "95thPercentile"),
                        (double) conn.getAttribute(requestsMBean, "98thPercentile"),
                        (double) conn.getAttribute(requestsMBean, "99thPercentile"));

            } else {
                return null;
            }
        } catch (IOException | MBeanException | AttributeNotFoundException | InstanceNotFoundException | ReflectionException ex) {
            LOG.error("", ex);
            return null;
        }
    }

    @Override
    public void stop() {
        if (connector != null) {
            try {
                connector.close();
            } catch (IOException e) {
                LOG.warn("Failed to close JMX connector", e);
            }
        }
    }
}

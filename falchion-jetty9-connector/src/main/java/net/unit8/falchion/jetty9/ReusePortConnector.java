package net.unit8.falchion.jetty9;

import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.annotation.Name;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;

/**
 * @author kawasima
 */
public class ReusePortConnector extends ServerConnector {
    public ReusePortConnector(@Name("server") Server server) {
        super(server);
    }

    public ReusePortConnector(@Name("server") Server server, @Name("factories") ConnectionFactory... factories) {
        super(server, factories);
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

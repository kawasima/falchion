package net.unit8.falchion.example;

import enkan.component.ApplicationComponent;
import enkan.component.falchion.FalchionStartNotifier;
import enkan.component.freemarker.FreemarkerTemplateEngine;
import enkan.component.jetty.JettyComponent;
import enkan.config.EnkanSystemFactory;
import enkan.system.EnkanSystem;
import net.unit8.falchion.jetty9.ReusePortConnector;
import org.eclipse.jetty.server.ServerConnector;

import static enkan.component.ComponentRelationship.component;
import static enkan.util.BeanBuilder.builder;

public class ExampleSystemFactory implements EnkanSystemFactory {
    @Override
    public EnkanSystem create() {
        return EnkanSystem.of(
                "http", builder(new JettyComponent())
                        .set(JettyComponent::setServerConnectorFactory, (server, options) -> {
                            ServerConnector connector = new ReusePortConnector(server);
                            connector.setPort(3000);
                            return connector;
                        })
                        .build(),
                "app", new ApplicationComponent("net.unit8.falchion.example.ExampleApplicationFactory"),
                "template", new FreemarkerTemplateEngine(),
                "falchion", new FalchionStartNotifier()
        ).relationships(
                component("falchion").using("http"),
                component("http").using("app"),
                component("app").using("template")
        );
    }
}

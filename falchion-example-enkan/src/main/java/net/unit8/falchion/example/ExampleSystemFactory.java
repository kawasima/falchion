package net.unit8.falchion.example;

import enkan.Env;
import enkan.component.ApplicationComponent;
import enkan.component.falchion.FalchionStartNotifier;
import enkan.component.freemarker.FreemarkerTemplateEngine;
import enkan.component.jetty.JettyComponent;
import enkan.component.metrics.MetricsComponent;
import enkan.config.EnkanSystemFactory;
import enkan.system.EnkanSystem;
import net.unit8.falchion.jetty9.ReusePortConnector;
import org.eclipse.jetty.server.ServerConnector;

import java.util.Objects;

import static enkan.component.ComponentRelationship.component;
import static enkan.util.BeanBuilder.builder;

public class ExampleSystemFactory implements EnkanSystemFactory {
    @Override
    public EnkanSystem create() {
        EnkanSystem system =  EnkanSystem.of(
                "http", builder(new JettyComponent())
                        .set(JettyComponent::setServerConnectorFactory, (server, options) -> {
                            ServerConnector connector = new ReusePortConnector(server);
                            connector.setPort(3000);
                            return connector;
                        })
                        .build(),
                "app", new ApplicationComponent("net.unit8.falchion.example.ExampleApplicationFactory"),
                "template", new FreemarkerTemplateEngine(),
                "metrics", new MetricsComponent()
        ).relationships(
                component("http").using("app"),
                component("app").using("template")
        );

        if (!Objects.equals(Env.get("enkan.env"), "repl")) {
            system.setComponent("falchion", new FalchionStartNotifier());
            system.relationships(component("falchion").using("http"));
        }

        return system;
    }
}

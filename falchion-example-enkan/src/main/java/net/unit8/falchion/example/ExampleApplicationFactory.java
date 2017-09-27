package net.unit8.falchion.example;

import enkan.Application;
import enkan.application.WebApplication;
import enkan.config.ApplicationFactory;
import enkan.endpoint.ResourceEndpoint;
import enkan.middleware.*;
import enkan.system.inject.ComponentInjector;
import kotowari.middleware.ControllerInvokerMiddleware;
import kotowari.middleware.FormMiddleware;
import kotowari.middleware.RenderTemplateMiddleware;
import kotowari.middleware.RoutingMiddleware;
import kotowari.routing.Routes;
import net.unit8.falchion.example.controller.IndexController;

import static enkan.util.Predicates.NONE;

public class ExampleApplicationFactory implements ApplicationFactory {
    @Override
    public Application create(ComponentInjector injector) {
        WebApplication app = new WebApplication();

        Routes routes = Routes.define(r -> {
            r.get("/").to(IndexController.class, "index");
        }).compile();


        // Enkan
        app.use(new DefaultCharsetMiddleware());
        app.use(NONE, new ServiceUnavailableMiddleware<>(new ResourceEndpoint("/public/html/503.html")));
        app.use(new TraceMiddleware<>());
        app.use(new ContentTypeMiddleware());
        app.use(new ParamsMiddleware());
        app.use(new MultipartParamsMiddleware());
        app.use(new MethodOverrideMiddleware());
        app.use(new NormalizationMiddleware());
        app.use(new NestedParamsMiddleware());
        app.use(new CookiesMiddleware());
        app.use(new ResourceMiddleware());
        app.use(new RenderTemplateMiddleware());
        app.use(new RoutingMiddleware(routes));
        app.use(new FormMiddleware());
        app.use(new ControllerInvokerMiddleware(injector));

        return app;
    }
}

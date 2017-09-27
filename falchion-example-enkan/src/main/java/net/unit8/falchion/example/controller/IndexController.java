package net.unit8.falchion.example.controller;

import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;

import javax.inject.Inject;

public class IndexController {
    @Inject
    private TemplateEngine templateEngine;

    public HttpResponse index() {
        return templateEngine.render("index");
    }
}

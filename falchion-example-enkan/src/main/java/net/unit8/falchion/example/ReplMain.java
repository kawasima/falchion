package net.unit8.falchion.example;

import enkan.system.command.MetricsCommandRegister;
import enkan.system.repl.PseudoRepl;
import enkan.system.repl.ReplBoot;
import enkan.system.repl.pseudo.ReplClient;
import kotowari.system.KotowariCommandRegister;

public class ReplMain {
    public static void main(String[] args) throws Exception {
        PseudoRepl repl = new PseudoRepl(ExampleSystemFactory.class.getName());
        ReplBoot.start(repl,
                new KotowariCommandRegister(),
                new MetricsCommandRegister());

        new ReplClient().start(repl.getPort().get());

    }
}

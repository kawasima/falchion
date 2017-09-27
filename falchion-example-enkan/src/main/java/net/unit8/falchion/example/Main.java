package net.unit8.falchion.example;

import enkan.system.EnkanSystem;

public class Main {
    public static void main(String[] args) {
        ExampleSystemFactory systemFactory = new ExampleSystemFactory();
        final EnkanSystem system = systemFactory.create();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> system.stop()));
        system.start();
    }
}

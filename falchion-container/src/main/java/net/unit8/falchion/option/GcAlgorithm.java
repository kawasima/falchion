package net.unit8.falchion.option;

import java.util.Random;

/**
 * Available GC algorithms for JVM processes.
 *
 * @author kawasima
 */
public enum GcAlgorithm {
    PARALLEL("-XX:+UseParallelGC"),
    G1("-XX:+UseG1GC"),
    ZGC("-XX:+UseZGC"),
    SHENANDOAH("-XX:+UseShenandoahGC");

    private static final Random RANDOM = new Random();

    private final String option;

    GcAlgorithm(String option) {
        this.option = option;
    }

    public String getOption() {
        return option;
    }

    public static GcAlgorithm random() {
        GcAlgorithm[] values = values();
        return values[RANDOM.nextInt(values.length)];
    }
}

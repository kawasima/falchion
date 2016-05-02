package net.unit8.falchion.option;

import java.util.Random;

/**
 * @author kawasima
 */
public class LongSampler {
    private Random random = new Random();
    private long init;
    private double variance = 0.0;

    public LongSampler(long init) {
        this.init = init;
    }

    public long getValue() {
        return init + (long) (random.nextGaussian() * variance);
    }

    public void setVariance(double variance) {
        this.variance = variance;
    }
}

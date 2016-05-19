package net.unit8.falchion.option.sampler;

import java.util.Random;

/**
 * @author kawasima
 */
public class LongSampler {
    private Random random = new Random();
    private long init;
    private double variance = 0.0;
    private Long min;
    private Long max;

    public LongSampler(long init) {
        this(init, 0.0);
    }

    public LongSampler(long init, double variance) {
        this(init, variance, null, null);
    }

    public LongSampler(long init, double variance, Long min, Long max) {
        this.init = init;
        this.variance = variance;
        this.min = min;
        this.max = max;
    }

    public long getValue() {
        long val = init + (long) (random.nextGaussian() * variance);
        if (min != null && val < min) {
            val = min;
        }
        if (max != null && val > max) {
            val = max;
        }
        return val;
    }

    public void setVariance(double variance) {
        this.variance = variance;
    }

    public void setMax(long max) {
        this.max = max;
    }

    public void setMin(long min) {
        this.min = min;
    }

    @Override
    public String toString() {
        return "LongSampler{" +
                "init=" + init +
                ", variance=" + variance +
                ", min=" + min +
                ", max=" + max +
                '}';
    }
}

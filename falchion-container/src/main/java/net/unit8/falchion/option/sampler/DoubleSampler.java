package net.unit8.falchion.option.sampler;

import java.util.Random;

/**
 * @author kawasima
 */
public class DoubleSampler {
    private Random random = new Random();
    private double init;
    private double variance = 0.0;
    private Double min;
    private Double max;

    public DoubleSampler(double init) {
        this.init = init;
    }

    public double getValue() {
        double val = init + (random.nextGaussian() * variance);
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

    public void setMax(double max) {
        this.max = max;
    }

    public void setMin(double min) {
        this.min = min;
    }
}

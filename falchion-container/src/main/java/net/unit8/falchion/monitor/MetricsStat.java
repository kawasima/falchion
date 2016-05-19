package net.unit8.falchion.monitor;

import javax.json.Json;
import javax.json.JsonObject;

/**
 * @author kawasima
 */
public class MetricsStat implements MonitorStat {
    private long count;
    private double mean;
    private double meanRate;
    private double min;
    private double max;
    private double stdDev;
    private double percentile95;
    private double percentile98;
    private double percentile99;

    public MetricsStat(long count, double mean, double meanRate, double min, double max, double stdDev, double percentile95, double percentile98, double percentile99) {
        this.count = count;
        this.mean = mean;
        this.meanRate = meanRate;
        this.min = min;
        this.max = max;
        this.stdDev = stdDev;
        this.percentile95 = percentile95;
        this.percentile98 = percentile98;
        this.percentile99 = percentile99;
    }

    @Override
    public JsonObject toJson() {
        return Json.createObjectBuilder()
                .add("type", "metrics")
                .add("count", count)
                .add("min", min)
                .add("mean", mean)
                .add("max", max)
                .add("meanRate", meanRate)
                .add("stdDev", stdDev)
                .add("percentile95", percentile95)
                .add("percentile98", percentile98)
                .add("percentile99", percentile99)
                .build();
    }

    public long getCount() {
        return count;
    }

    public double getMean() {
        return mean;
    }

    public double getMeanRate() {
        return meanRate;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStdDev() {
        return stdDev;
    }

    public double getPercentile95() {
        return percentile95;
    }

    public double getPercentile98() {
        return percentile98;
    }

    public double getPercentile99() {
        return percentile99;
    }
}

package net.unit8.falchion.option.provider;

import net.unit8.falchion.option.GcAlgorithm;
import net.unit8.falchion.option.JvmType;
import net.unit8.falchion.option.sampler.LongSampler;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

/**
 * @author kawasima
 */
public class StandardOptionProvider implements OptionProvider {
    private JvmType jvmType = JvmType.SERVER;
    private GcAlgorithm gcAlgorithm = GcAlgorithm.PARALLEL;
    private LongSampler initialHeap;
    private LongSampler maxHeap;
    private LongSampler maxHeapFreeRatio;
    private LongSampler minHeapFreeRatio;
    private LongSampler newRatio;
    private LongSampler survivorRatio;

    private static final Set<StandardOption> OPTION_SET = new HashSet<>(Arrays.asList(
            new StandardOption("-Xms", 32, 256 * 1024, (sop, ls) -> sop.initialHeap = ls),
            new StandardOption("-Xmx", 32, 256 * 1024, (sop, ls) -> sop.maxHeap = ls),
            new StandardOption("-XX:MaxHeapFreeRatio=", 0, 100, (sop, ls) -> sop.maxHeapFreeRatio = ls),
            new StandardOption("-XX:MinHeapFreeRatio=", 0, 100, (sop, ls) -> sop.minHeapFreeRatio = ls),
            new StandardOption("-XX:NewRatio=", 1, 4, (sop, ls) -> sop.newRatio = ls),
            new StandardOption("-XX:SurvivorRatio=", 1, 16, (sop, ls) -> sop.survivorRatio = ls)
    ));

    public StandardOptionProvider(long initialHeap, long maxHeap) {
        this.initialHeap = new LongSampler(initialHeap);
        this.maxHeap = new LongSampler(maxHeap);
        this.maxHeapFreeRatio = new LongSampler(70, 0, 0L, 100L);
        this.minHeapFreeRatio = new LongSampler(40, 0, 0L, 100L);
        this.newRatio = new LongSampler(2, 0, 1L, 4L);
        this.survivorRatio = new LongSampler(8, 0, 1L, 16L);
    }

    public StandardOptionProvider(long initialHeap, long maxHeap, double coefficientOfVariance) {
        this.initialHeap = new LongSampler(initialHeap);
        this.maxHeap = new LongSampler(maxHeap);

        this.maxHeapFreeRatio = new LongSampler(70, coefficientOfVariance * 5, 0L, 100L);
        this.minHeapFreeRatio = new LongSampler(40, coefficientOfVariance * 5, 0L, 100L);
        this.newRatio = new LongSampler(2, coefficientOfVariance * 1, 1L, 4L);
        this.survivorRatio = new LongSampler(8, coefficientOfVariance * 4, 1L, 16L);
    }

    public StandardOptionProvider(String javaOpts, double coefficientOfVariance) {
        if (javaOpts != null) {
            Arrays.stream(javaOpts.split("\\s+"))
                    .filter(s -> !s.isEmpty())
                    .forEach(s -> OPTION_SET.stream()
                            .filter(opt -> s.startsWith(opt.getPrefix()))
                            .forEach(opt -> opt.setValue(this, s, coefficientOfVariance))
                    );
        }
    }

    @Override
    public List<String> getOptions() {
        List<String> options = new ArrayList<>();
        options.add(jvmType.getOptionValue());
        options.add("-XX:+StartAttachListener");
        options.add(gcAlgorithm.getOption());

        Long maxHeapFreeRatioValue = null;

        if (initialHeap != null)
            options.add("-Xms" + initialHeap.getValue() + "m");

        if (maxHeap != null)
            options.add("-Xmx" + maxHeap.getValue() + "m");

        if (maxHeapFreeRatio != null) {
            maxHeapFreeRatioValue = maxHeapFreeRatio.getValue();
            options.add("-XX:MaxHeapFreeRatio=" + maxHeapFreeRatioValue);
        }

        if (minHeapFreeRatio != null) {
            long min = minHeapFreeRatio.getValue();
            if (maxHeapFreeRatioValue != null && min > maxHeapFreeRatioValue)
                min = maxHeapFreeRatioValue;
            options.add("-XX:MinHeapFreeRatio=" + min);
        }

        if (newRatio != null)
            options.add("-XX:NewRatio=" + newRatio.getValue());

        if (survivorRatio != null)
            options.add("-XX:SurvivorRatio=" + survivorRatio.getValue());
        return options;
    }

    public void setGcAlgorithm(GcAlgorithm gcAlgorithm) {
        this.gcAlgorithm = gcAlgorithm;
    }

    private static class StandardOption {
        StandardOption(String prefix, long min, long max, BiConsumer<StandardOptionProvider, LongSampler> setter) {
            this.prefix = prefix;
            this.min = min;
            this.max = max;
            this.setter = setter;
        }

        private String prefix;
        private Long min;
        private Long max;
        private BiConsumer<StandardOptionProvider, LongSampler> setter;

        public String getPrefix() {
            return prefix;
        }

        public void setValue(StandardOptionProvider sop, String optionString, double variance) {
            String val = optionString.substring(prefix.length());

            if (Pattern.matches("[0-9]+", val)) {
                setter.accept(sop, new LongSampler(Long.parseLong(val), (max - min) * variance, min, max));
            } else if (Pattern.matches("[0-9]+[Mm]", val)) {
                setter.accept(sop, new LongSampler(Long.parseLong(val.substring(0, val.length() - 1)), 0.0, min, max));
            } else if (Pattern.matches("[0-9]+[Gg]", val)) {
                setter.accept(sop, new LongSampler(Long.parseLong(val.substring(0, val.length() - 1)) * 1024, 0.0, min, max));
            } else {
                throw new IllegalArgumentException(optionString);
            }
        }
    }
}

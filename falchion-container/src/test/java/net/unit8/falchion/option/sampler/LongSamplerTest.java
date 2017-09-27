package net.unit8.falchion.option.sampler;

import org.junit.Test;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author kawasima
 */
public class LongSamplerTest {
    @Test
    public void test() {
        LongSampler sampler = new LongSampler(5);
        sampler.setVariance(3);

        Map<Long, Long> histo = Stream.generate(sampler::getValue)
                .limit(10000)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        histo.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(e -> System.out.println(e.getKey() + ":" + e.getValue()));
    }
}

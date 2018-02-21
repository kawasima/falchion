package net.unit8.falchion.option.sampler;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kawasima
 */
public class LongSamplerTest {
    @Test
    public void test() {
        LongSampler sampler = new LongSampler(5);
        sampler.setVariance(3);

        Map<Long, Long> histograms = Stream.generate(sampler::getValue)
                .limit(10000)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        Optional<Map.Entry<Long, Long>> max = histograms.entrySet().stream()
            .max(Comparator.comparingLong(Map.Entry::getValue));
        assertThat(max).isPresent();
        assertThat(max.get().getKey()).isEqualTo(5);
    }
}

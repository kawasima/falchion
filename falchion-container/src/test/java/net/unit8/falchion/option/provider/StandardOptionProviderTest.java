package net.unit8.falchion.option.provider;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kawasima
 */
public class StandardOptionProviderTest {
    @Test
    public void test() {
        assertThat(new StandardOptionProvider("-XX:MaxHeapFreeRatio=3", 0.0).getOptions())
                .contains("-XX:MaxHeapFreeRatio=3");
    }
}

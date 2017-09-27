package net.unit8.falchion.option.provider;

import org.junit.Test;

/**
 * @author kawasima
 */
public class StandardOptionProviderTest {
    @Test
    public void test() {
        System.out.println(new StandardOptionProvider("-XX:MaxHeapFreeRatio=3", 0.0).getOptions());
    }
}

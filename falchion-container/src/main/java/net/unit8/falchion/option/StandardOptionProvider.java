package net.unit8.falchion.option;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kawasima
 */
public class StandardOptionProvider implements OptionProvider {
    private JvmType jvmType = JvmType.SERVER;
    private LongSampler initialHeap;
    private LongSampler maxHeap;

    public StandardOptionProvider(long initialHeap, long maxHeap) {
        this.initialHeap = new LongSampler(initialHeap);
        this.maxHeap = new LongSampler(maxHeap);
    }

    @Override
    public List<String> getOptions() {
        List<String> options = new ArrayList<>();
        options.add(jvmType.getOptionValue());

        return options;
    }
}

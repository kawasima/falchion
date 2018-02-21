package net.unit8.falchion.supplier;

import net.unit8.falchion.JvmProcess;
import net.unit8.falchion.evaluator.Evaluator;
import net.unit8.falchion.option.provider.StandardOptionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author kawasima
 */
public class AutoOptimizableProcessSupplier implements Supplier<JvmProcess> {
    private static final Logger LOG = LoggerFactory.getLogger(AutoOptimizableProcessSupplier.class);

    private StandardOptionProvider standardOptionProvider;
    private Evaluator evaluator;
    private Supplier<JvmProcess> baseSupplier;
    private double variance = 0.1;

    public AutoOptimizableProcessSupplier(Supplier<JvmProcess> baseSupplier, Evaluator evaluator) {
        this.baseSupplier = baseSupplier;
        this.evaluator = evaluator;
        standardOptionProvider = new StandardOptionProvider(128, 128, variance);
    }

    @Override
    public JvmProcess get() {
        JvmProcess process = baseSupplier.get();
        List<String> options = new ArrayList<>(standardOptionProvider.getOptions());
        process.setJvmOptions(options);
        return process;
    }

    public void feedback(Collection<JvmProcess> processes) {
        JvmProcess best = evaluator.evaluate(processes);
        LOG.info("best param {}", best.getJvmOptions());
        standardOptionProvider = new StandardOptionProvider(String.join(" ", best.getJvmOptions()), variance);
    }
}

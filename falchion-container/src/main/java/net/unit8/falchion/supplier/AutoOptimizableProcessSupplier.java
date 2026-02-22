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
    private final Evaluator evaluator;
    private final Supplier<JvmProcess> baseSupplier;
    private final double variance;
    private final List<List<String>> tuningHistory = new ArrayList<>();

    public AutoOptimizableProcessSupplier(Supplier<JvmProcess> baseSupplier, Evaluator evaluator) {
        this.baseSupplier = baseSupplier;
        this.evaluator = evaluator;
        this.variance = 0.1;
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
        tuningHistory.add(new ArrayList<>(best.getJvmOptions()));
        standardOptionProvider = new StandardOptionProvider(String.join(" ", best.getJvmOptions()), variance);
    }

    public void printTuningSummary() {
        if (tuningHistory.isEmpty()) {
            LOG.info("[Auto Tuning Summary] No feedback rounds executed");
            return;
        }

        LOG.info("=== Auto Tuning Summary ({} rounds) ===", tuningHistory.size());
        for (int i = 0; i < tuningHistory.size(); i++) {
            LOG.info("  Round {}: {}", i + 1, tuningHistory.get(i));
        }
        LOG.info("  Final:   {}", tuningHistory.get(tuningHistory.size() - 1));
        LOG.info("============================================");
    }
}

package net.unit8.falchion.supplier;

import net.unit8.falchion.JvmProcess;
import net.unit8.falchion.evaluator.Evaluator;
import net.unit8.falchion.option.GcAlgorithm;
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
    private final boolean autoSelectGc;
    private final List<List<String>> tuningHistory = new ArrayList<>();

    public AutoOptimizableProcessSupplier(Supplier<JvmProcess> baseSupplier, Evaluator evaluator) {
        this(baseSupplier, evaluator, 0.1, true);
    }

    public AutoOptimizableProcessSupplier(Supplier<JvmProcess> baseSupplier, Evaluator evaluator, double variance) {
        this(baseSupplier, evaluator, variance, true);
    }

    public AutoOptimizableProcessSupplier(Supplier<JvmProcess> baseSupplier, Evaluator evaluator, double variance, boolean autoSelectGc) {
        this.baseSupplier = baseSupplier;
        this.evaluator = evaluator;
        this.variance = variance;
        this.autoSelectGc = autoSelectGc;
        standardOptionProvider = new StandardOptionProvider(128, 128, variance);
    }

    @Override
    public JvmProcess get() {
        JvmProcess process = baseSupplier.get();
        if (autoSelectGc) {
            standardOptionProvider.setGcAlgorithm(GcAlgorithm.random());
        }
        List<String> options = new ArrayList<>(standardOptionProvider.getOptions());
        process.setJvmOptions(options);
        return process;
    }

    public void feedback(Collection<JvmProcess> processes) {
        JvmProcess best = evaluator.evaluate(processes);
        LOG.info("best param {}", best.getJvmOptions());
        tuningHistory.add(new ArrayList<>(best.getJvmOptions()));
        double decayedVariance = variance * (1.0 / (1 + tuningHistory.size() * 0.2));
        LOG.info("variance: {} -> {} (round {})", variance, decayedVariance, tuningHistory.size());
        standardOptionProvider = new StandardOptionProvider(String.join(" ", best.getJvmOptions()), decayedVariance);
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

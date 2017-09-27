package net.unit8.falchion.evaluator;

import java.util.function.Supplier;

/**
 * @author kawasima
 */
public enum EvaluatorSupplier {
    MIN_GC_TIME(MinGcTime::new),
    MIN_FULL_GC_COUNT(MinFullGcCount::new);

    EvaluatorSupplier(Supplier<Evaluator> supplier) {
        this.supplier = supplier;
    }

    Supplier<Evaluator> supplier;

    public Evaluator createEvaluator() {
        return supplier.get();
    }
}

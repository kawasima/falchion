package net.unit8.falchion.evaluator;

import java.util.function.Supplier;

/**
 * @author kawasima
 */
public enum EvaluatorSupplier {
    MIN_GC_TIME(() -> new MinGcTime());

    EvaluatorSupplier(Supplier<Evaluator> supplier) {
        this.supplier = supplier;
    }

    Supplier<Evaluator> supplier;

    public Evaluator createEvaluator() {
        return supplier.get();
    }
}

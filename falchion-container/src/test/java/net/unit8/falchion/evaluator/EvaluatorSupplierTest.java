package net.unit8.falchion.evaluator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EvaluatorSupplierTest {
    @Test
    public void minGcTimeSupplierCreatesMinGcTimeInstance() {
        Evaluator evaluator = EvaluatorSupplier.MIN_GC_TIME.createEvaluator();
        assertThat(evaluator).isInstanceOf(MinGcTime.class);
    }

    @Test
    public void minFullGcCountSupplierCreatesMinFullGcCountInstance() {
        Evaluator evaluator = EvaluatorSupplier.MIN_FULL_GC_COUNT.createEvaluator();
        assertThat(evaluator).isInstanceOf(MinFullGcCount.class);
    }

    @Test
    public void valueOfResolvesEnumNames() {
        assertThat(EvaluatorSupplier.valueOf("MIN_GC_TIME"))
                .isEqualTo(EvaluatorSupplier.MIN_GC_TIME);
        assertThat(EvaluatorSupplier.valueOf("MIN_FULL_GC_COUNT"))
                .isEqualTo(EvaluatorSupplier.MIN_FULL_GC_COUNT);
    }
}

package net.unit8.falchion.evaluator;

import net.unit8.falchion.JvmProcess;

import java.util.Collection;

/**
 * @author kawasima
 */
@FunctionalInterface
public interface Evaluator {
    JvmProcess evaluate(Collection<JvmProcess> processes);
}

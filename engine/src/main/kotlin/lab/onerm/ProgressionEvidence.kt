package lab.onerm

import kotlin.math.abs

/** Pure, testable comparison of two completed working sets. */
object ProgressionEvidence {
    data class Set(val kg: Double, val reps: Int, val rir: Int?)
    data class Outcome(val incrementKg: Double, val tolerated: Boolean)

    fun compare(before: Set, after: Set): Outcome? {
        if (!before.kg.isFinite() || !after.kg.isFinite() || before.kg <= 0 || after.kg <= 0) return null
        if (before.reps !in 3..12 || after.reps !in 3..12) return null
        if (abs(before.reps - after.reps) > 2) return null
        // An outcome needs effort recorded both before and after the increase.
        if (before.rir == null || after.rir == null || before.rir !in 0..10 || after.rir !in 0..10) return null
        if (abs(before.rir - after.rir) > 3) return null
        val step = after.kg - before.kg
        if (step < 0.5 || step > 10.0) return null
        return Outcome(step, after.reps >= before.reps - 2 && after.rir >= 2)
    }

    fun selectStep(outcomes: List<Outcome>, defaultStep: Double): Double? {
        if (outcomes.size < 3) return null
        val successes = outcomes.filter { it.tolerated }
        if (successes.size < 2) return null
        val failures = outcomes.filterNot { it.tolerated }
        return successes.map { it.incrementKg }
            .filter { candidate -> failures.none { it.incrementKg <= candidate + 0.25 } }
            .minOrNull()?.coerceIn(1.0, defaultStep)
    }
}

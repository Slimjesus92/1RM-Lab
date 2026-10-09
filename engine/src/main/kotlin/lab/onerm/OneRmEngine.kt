package lab.onerm

import kotlin.math.abs

data class LiftSet(val weightKg: Double, val reps: Int, val rir: Int? = null)
data class SetEstimate(val epleyKg: Double, val brzyckiKg: Double, val combinedKg: Double)
data class SessionEstimate(val oneRmKg: Double, val setsUsed: Int, val spreadKg: Double)

object OneRmEngine {
    fun estimate(set: LiftSet): SetEstimate {
        require(set.weightKg.isFinite() && set.weightKg > 0.0) { "Weight must be positive" }
        require(set.reps in 1..15) { "Reps must be between 1 and 15" }
        require(set.rir == null || set.rir in 0..5) { "RIR must be between 0 and 5" }
        val effective = set.reps + (set.rir ?: 0)
        val epley = if (effective == 1) set.weightKg else set.weightKg * (1.0 + effective / 30.0)
        val brzycki = if (effective == 1) set.weightKg else set.weightKg * 36.0 / (37.0 - effective)
        return SetEstimate(epley, brzycki, (epley + brzycki) / 2.0)
    }
    fun session(sets: List<LiftSet>): SessionEstimate {
        require(sets.isNotEmpty()) { "Add at least one set" }
        val values = sets.map { estimate(it).combinedKg }.sorted()
        // Median resists a single anomalous set; spread is disagreement, NOT a confidence interval.
        val middle = values.size / 2
        val median = if (values.size % 2 == 1) values[middle] else (values[middle - 1] + values[middle]) / 2.0
        return SessionEstimate(median, values.size, values.last() - values.first())
    }
}
package lab.onerm

import kotlin.math.max
import kotlin.math.sqrt

/** Platform-independent trend model. The band is descriptive, not a calibrated confidence interval. */
object StrengthTrend {
    data class Observation(val day: Double, val estimatedMax: Double)
    data class Projection(val current: Double, val projected: Double, val low: Double, val high: Double)
    data class Backtest(val samples: Int, val meanAbsoluteErrorKg: Double, val withinBand: Int)

    fun project(history: List<Observation>, horizonDays: Double = 28.0): Projection? {
        val points = history.sortedBy { it.day }.takeLast(12)
        if (points.size < 4 || points.last().day - points.first().day < 21.0 ||
            horizonDays <= 0.0 || points.any { !it.day.isFinite() || !it.estimatedMax.isFinite() || it.estimatedMax <= 0.0 }) return null
        val xm = points.map { it.day }.average()
        val ym = points.map { it.estimatedMax }.average()
        val denominator = points.sumOf { (it.day - xm) * (it.day - xm) }
        if (denominator <= 0.0) return null
        val slope = points.sumOf { (it.day - xm) * (it.estimatedMax - ym) } / denominator
        val residual = sqrt(points.sumOf {
            val delta = it.estimatedMax - (ym + slope * (it.day - xm))
            delta * delta
        } / (points.size - 2))
        val current = points.last().estimatedMax
        val projected = (current + slope * horizonDays).coerceIn(current * 0.85, current * 1.15)
        val band = max(residual * 2.0, current * 0.05)
        return Projection(current, projected, max(0.0, projected - band), projected + band)
    }

    /** Walk-forward historical check: forecast from past only, compare to first observation at/after horizon. */
    fun backtest(history: List<Observation>, horizonDays: Double = 28.0): Backtest? {
        val points = history.sortedBy { it.day }
        val errors = mutableListOf<Double>()
        var covered = 0
        for (end in 4 until points.size) {
            val train = points.take(end)
            val forecast = project(train, horizonDays) ?: continue
            val targetDay = train.last().day + horizonDays
            val observed = points.drop(end).firstOrNull { it.day >= targetDay } ?: continue
            val error = kotlin.math.abs(forecast.projected - observed.estimatedMax)
            errors.add(error)
            if (observed.estimatedMax in forecast.low..forecast.high) covered++
        }
        if (errors.isEmpty()) return null
        return Backtest(errors.size, errors.average(), covered)
    }
}

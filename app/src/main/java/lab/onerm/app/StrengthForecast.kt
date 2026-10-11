package lab.onerm.app

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.sqrt

/** Descriptive short-horizon projection, not a guaranteed future max. */
object StrengthForecast {
    data class Forecast(val current: Double, val projected: Double, val low: Double,
        val high: Double, val sessions: Int, val explanation: String)
    fun calculate(all: JSONArray, exercise: String): Forecast? {
        val observations = mutableListOf<Pair<Long, Double>>()
        for (i in 0 until all.length()) {
            val session = all.optJSONObject(i) ?: continue
            val stamp = session.optLong("finished", 0L)
            if (stamp <= 0L || stamp > System.currentTimeMillis()) continue
            val sets = session.optJSONArray("sets") ?: continue
            val best = (0 until sets.length()).mapNotNull { sets.optJSONObject(it) }
                .filter { it.optString("exercise") == exercise && !it.optBoolean("warmup", false) }
                .filter { it.optDouble("kg") > 0 && it.optInt("reps") in 1..10 }
                .maxOfOrNull { it.optDouble("kg") * (1.0 + it.optInt("reps") / 30.0) }
            if (best != null) observations.add(stamp to best)
        }
        val sorted = observations.sortedBy { it.first }.takeLast(12)
        if (sorted.size < 4) return null
        val start = sorted.first().first
        val x = sorted.map { (it.first - start).toDouble() / 86400000.0 }
        if (x.last() - x.first() < 21.0) return null
        val y = sorted.map { it.second }
        val xm = x.average()
        val ym = y.average()
        val variance = x.sumOf { (it - xm) * (it - xm) }
        if (variance <= 0.0) return null
        val slope = x.indices.sumOf { (x[it] - xm) * (y[it] - ym) } / variance
        val residual = sqrt(x.indices.sumOf {
            val fitted = ym + slope * (x[it] - xm)
            (y[it] - fitted) * (y[it] - fitted)
        } / (x.size - 2))
        val current = y.last()
        val horizon = 28.0
        val projected = (current + slope * horizon).coerceIn(current * 0.85, current * 1.15)
        val band = maxOf(residual * 2.0, current * 0.05)
        return Forecast(current, projected, (projected - band).coerceAtLeast(0.0),
            projected + band, sorted.size,
            "Exploratory 4-week projection from recent session-best Epley estimates. " +
            "Range reflects historical variability (minimum ±5%), not a validated prediction interval.")
    }
}

package lab.onerm.app

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.sqrt

/** Descriptive short-horizon projection, not a guaranteed future max. */
object StrengthForecast {
    data class Forecast(val current: Double, val projected: Double, val low: Double,
        val high: Double, val sessions: Int, val explanation: String)
    fun observations(all: JSONArray, exercise: String): List<Pair<Long, Double>> {
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
        return observations.sortedBy { it.first }.takeLast(12)
    }

    fun calculate(all: JSONArray, exercise: String): Forecast? {
        val sorted = observations(all, exercise)
        if (sorted.size < 4) return null
        val start = sorted.first().first
        val history = sorted.map { (stamp, estimate) ->
            lab.onerm.StrengthTrend.Observation((stamp - start).toDouble() / 86400000.0, estimate)
        }
        val projection = lab.onerm.StrengthTrend.project(history) ?: return null
        return Forecast(projection.current, projection.projected, projection.low,
            projection.high, sorted.size,
            "Exploratory 4-week projection from session-best Epley estimates. " +
            "Range reflects historical variability (minimum ±5%), not a validated prediction interval.")
    }

    fun backtest(all: JSONArray, exercise: String): lab.onerm.StrengthTrend.Backtest? {
        val sorted = observations(all, exercise)
        if (sorted.isEmpty()) return null
        val start = sorted.first().first
        return lab.onerm.StrengthTrend.backtest(sorted.map { (stamp, estimate) ->
            lab.onerm.StrengthTrend.Observation((stamp - start).toDouble() / 86400000.0, estimate)
        })
    }
}

package lab.onerm.app

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.round

/**
 * Conservative training suggestion. Uses only completed working sets.
 * No fatigue, recovery or strength gains are inferred from missing data.
 */
object StrengthIntelligence {
    data class Suggestion(
        val weightKg: Double,
        val reps: Int,
        val targetRir: Int,
        val explanation: String,
        val trend: String
    )

    fun suggest(sessions: JSONArray, exercise: String): Suggestion? {
        val workouts = mutableListOf<List<JSONObject>>()
        for (i in sessions.length() - 1 downTo 0) {
            val sets = sessions.optJSONObject(i)?.optJSONArray("sets") ?: continue
            val working = (0 until sets.length()).mapNotNull { sets.optJSONObject(it) }
                .filter { it.optString("exercise") == exercise && !it.optBoolean("warmup", false) &&
                    it.optDouble("kg") > 0 && it.optInt("reps") in 1..30 }
            if (working.isNotEmpty()) workouts.add(working)
            if (workouts.size == 3) break
        }
        if (workouts.isEmpty()) return null
        val last = workouts.first()
        val reference = last.maxByOrNull { it.optDouble("kg") * (1 + it.optInt("reps") / 30.0) } ?: return null
        val kg = reference.optDouble("kg")
        val reps = reference.optInt("reps")
        val hasRir = !reference.isNull("rir") && reference.has("rir")
        val rir = if (hasRir) reference.optInt("rir").coerceIn(0, 10) else 2
        val prior = workouts.getOrNull(1)?.maxByOrNull {
            it.optDouble("kg") * (1 + it.optInt("reps") / 30.0)
        }
        val priorEstimate = prior?.let { it.optDouble("kg") * (1 + it.optInt("reps") / 30.0) }
        val estimate = kg * (1 + reps / 30.0)
        val trend = when {
            priorEstimate == null -> "First recorded session"
            estimate > priorEstimate * 1.015 -> "Performance trending up"
            estimate < priorEstimate * 0.985 -> "Performance below previous session"
            else -> "Performance broadly stable"
        }
        val consistent = workouts.size >= 2 && prior != null &&
            prior.optDouble("kg") >= kg * 0.98 &&
            prior.optInt("reps") >= reps &&
            (!prior.has("rir") || prior.isNull("rir") || prior.optInt("rir") >= 2)
        val increase = hasRir && rir >= 3 && consistent && reps in 3..12
        val target = if (increase) round((kg + 2.5) / 2.5) * 2.5 else kg
        val reason = when {
            !hasRir -> "RIR wasn't logged for the reference set; hold weight until effort is clearer."
            increase -> "Two comparable sessions and at least 3 reps in reserve support a cautious 2.5 kg increase."
            rir <= 1 -> "Last set was close to failure; repeat the load before increasing."
            workouts.size < 2 -> "Only one session available; establish a repeatable baseline first."
            else -> "Maintain this weight and build consistent reps with around 2 RIR."
        }
        return Suggestion(target, if (increase) (reps - 1).coerceAtLeast(3) else reps,
            2, reason, trend)
    }
}

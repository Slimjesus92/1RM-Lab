package lab.onerm.app

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.*
import lab.onerm.LiftSet
import lab.onerm.OneRmEngine
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private val sets = mutableListOf<LiftSet>()
    private lateinit var rows: LinearLayout
    private lateinit var result: TextView
    private lateinit var exercise: Spinner
    private lateinit var history: TextView
    private lateinit var chart: ProgressChart
    private lateinit var widgetExercise: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 20)
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
        root.addView(TextView(this).apply { text = "1RM LAB"; textSize = 28f })
        root.addView(TextView(this).apply { text = "Research-informed strength estimates • kg"; textSize = 14f })
        exercise = Spinner(this)
        val names = listOf("Bench press", "Squat", "Deadlift", "Overhead press", "Barbell row", "Other")
        exercise.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        root.addView(exercise)
        val weight = edit("Weight (kg)", true)
        val reps = edit("Repetitions (1–15)", false)
        val rir = edit("RIR (optional, 0–5)", false)
        root.addView(weight); root.addView(reps); root.addView(rir)
        val add = Button(this).apply { text = "Add set" }
        root.addView(add)
        rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(rows)
        result = TextView(this).apply { text = "Add a set to estimate your 1RM"; textSize = 22f; setPadding(0, 18, 0, 18) }
        root.addView(result)
        root.addView(Button(this).apply {
            text = "Save session"
            setOnClickListener {
                if (sets.isEmpty()) { Toast.makeText(this@MainActivity, "Add a set first", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                saveRecord(exercise.selectedItem.toString(), sets.maxOf { OneRmEngine.estimate(it).combinedKg }, "estimated")
                showHistory()
            }
        })
        history = TextView(this).apply { textSize = 17f; setPadding(0, 24, 0, 16) }
        root.addView(history)
        chart = ProgressChart(this)
        root.addView(chart)
        root.addView(TextView(this).apply { text = "WIDGET EXERCISE"; textSize = 18f })
        widgetExercise = Spinner(this)
        widgetExercise.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        root.addView(widgetExercise)
        val widgetPrefs = getSharedPreferences("last_estimate", MODE_PRIVATE)
        widgetExercise.setSelection(names.indexOf(widgetPrefs.getString("widget_exercise", names[0])).coerceAtLeast(0))
        widgetExercise.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                widgetPrefs.edit().putString("widget_exercise", names[position]).apply()
                updateWidget()
            }
        }
        root.addView(TextView(this).apply { text = "ACTUAL 1RM (TESTED LIFT)"; textSize = 18f; setPadding(0, 18, 0, 0) })
        val actualWeight = edit("Weight lifted for 1 rep (kg)", true)
        root.addView(actualWeight)
        root.addView(Button(this).apply {
            text = "Save actual 1RM"
            setOnClickListener {
                val value = actualWeight.text.toString().toDoubleOrNull()
                if (value == null || !value.isFinite() || value <= 0.0) {
                    Toast.makeText(this@MainActivity, "Enter a valid lifted weight", Toast.LENGTH_SHORT).show()
                } else {
                    saveRecord(exercise.selectedItem.toString(), value, "actual")
                    actualWeight.text.clear()
                    showHistory()
                }
            }
        })
        exercise.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                sets.clear(); refresh(); showHistory()
            }
        }
        showHistory()
        root.addView(TextView(this).apply { text = "Estimates are not measured 1RMs. Unknown RIR is provisionally treated as 0 for calculation, and should not be interpreted as confirmed failure. Set spread is not a statistical confidence interval." })
        add.setOnClickListener {
            try {
                val w = weight.text.toString().toDouble()
                val r = reps.text.toString().toInt()
                val reserve = rir.text.toString().takeIf { it.isNotBlank() }?.toInt()
                val set = LiftSet(w, r, reserve)
                OneRmEngine.estimate(set)
                sets.add(set)
                refresh()
            } catch (e: Exception) {
                Toast.makeText(this, e.message ?: "Check inputs", Toast.LENGTH_LONG).show()
            }
        }
        root.addView(Button(this).apply { text = "Clear session"; setOnClickListener { sets.clear(); refresh() } })
    }
    private fun records(): JSONArray {
        val raw = getSharedPreferences("strength_history", MODE_PRIVATE).getString("records", "[]") ?: "[]"
        return try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
    }
    private fun saveRecord(name: String, value: Double, type: String) {
        val all = records()
        all.put(JSONObject().put("exercise", name).put("value", value)
            .put("date", System.currentTimeMillis()).put("type", type))
        getSharedPreferences("strength_history", MODE_PRIVATE).edit().putString("records", all.toString()).apply()
        updateWidget()
    }
    private fun updateWidget() {
        val prefs = getSharedPreferences("last_estimate", MODE_PRIVATE)
        val name = prefs.getString("widget_exercise", "Bench press") ?: "Bench press"
        val all = records()
        val estimates = (0 until all.length()).mapNotNull { i ->
            all.optJSONObject(i)?.takeIf {
                it.optString("exercise") == name && it.optString("type", "estimated") != "actual"
            }?.optDouble("value")?.takeIf { it.isFinite() && it > 0 }
        }
        val label = if (estimates.isEmpty()) "$name: No saved estimate"
            else "$name: " + String.format(Locale.UK, "%.1f kg", estimates.maxOrNull())
        prefs.edit().putString("label", label).apply()
        val manager = AppWidgetManager.getInstance(this)
        OneRmWidget().onUpdate(this, manager, manager.getAppWidgetIds(ComponentName(this, OneRmWidget::class.java)))
    }
    private fun showHistory() {
        if (!::history.isInitialized) return
        val all = records()
        val selected = exercise.selectedItem?.toString() ?: return
        val matching = (0 until all.length()).mapNotNull { i ->
            all.optJSONObject(i)?.takeIf { it.optString("exercise") == selected }
        }
        val estimates = matching.filter { it.optString("type", "estimated") != "actual" }
            .map { it.optDouble("value") }.filter { it.isFinite() && it > 0 }
        val actuals = matching.filter { it.optString("type") == "actual" }
            .map { it.optDouble("value") }.filter { it.isFinite() && it > 0 }
        chart.points = matching.filter { it.optString("type", "estimated") != "actual" }
            .mapNotNull {
                val value = it.optDouble("value")
                if (value.isFinite() && value > 0) ProgressChart.Point(value, it.optLong("date", System.currentTimeMillis())) else null
            }
        val format = { value: Double -> String.format(Locale.UK, "%.1f kg", value) }
        val estimateLabel = if (estimates.isEmpty()) "No estimated sessions saved"
            else "Estimated best: " + format(estimates.maxOrNull() ?: 0.0) +
                "\nLatest estimate: " + format(estimates.last()) +
                "\nChange: " + String.format(Locale.UK, "%+.1f kg", estimates.last() - estimates.first()) +
                "\nEstimated sessions: " + estimates.size
        val actualLabel = if (actuals.isEmpty()) "No tested 1RM saved"
            else "Tested 1RM personal best: " + format(actuals.maxOrNull() ?: 0.0)
        history.text = "STRENGTH DASHBOARD — $selected\n\n$estimateLabel\n\n$actualLabel\n\nEstimated 1RM progression:"
    }
    private fun edit(hintText: String, decimal: Boolean) = EditText(this).apply {
        hint = hintText
        inputType = if (decimal) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_CLASS_NUMBER
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    private fun refresh() {
        rows.removeAllViews()
        sets.forEachIndexed { index, set ->
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val description = TextView(this).apply {
                text = "${index + 1}. ${set.weightKg} kg × ${set.reps} · RIR ${set.rir?.toString() ?: "unknown"}\nSet 1RM: ${String.format(Locale.UK, "%.1f", OneRmEngine.estimate(set).combinedKg)} kg"
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            line.addView(description)
            line.addView(Button(this).apply { text = "Remove"; setOnClickListener { sets.removeAt(index); refresh() } })
            rows.addView(line)
        }
        if (sets.isEmpty()) { result.text = "Add a set to estimate your 1RM"; return }
        val estimate = OneRmEngine.session(sets)
        val strongest = sets.maxOf { OneRmEngine.estimate(it).combinedKg }
        val label = String.format(Locale.UK, "%.1f kg", strongest)
        result.text = "Strongest estimated 1RM: $label\nSession median: ${String.format(Locale.UK, "%.1f", estimate.oneRmKg)} kg (${estimate.setsUsed} sets)\nSet disagreement: ${String.format(Locale.UK, "%.1f", estimate.spreadKg)} kg"
        // The widget follows its selected exercise and saved history.
        updateWidget()
    }
}
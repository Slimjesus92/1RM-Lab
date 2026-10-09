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
                val prefs = getSharedPreferences("strength_history", MODE_PRIVATE)
                val records = JSONArray(prefs.getString("records", "[]"))
                records.put(JSONObject().put("exercise", exercise.selectedItem.toString())
                    .put("value", sets.maxOf { OneRmEngine.estimate(it).combinedKg })
                    .put("date", System.currentTimeMillis()))
                prefs.edit().putString("records", records.toString()).apply()
                showHistory()
            }
        })
        history = TextView(this).apply { textSize = 17f; setPadding(0, 24, 0, 16) }
        root.addView(history)
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
    private fun showHistory() {
        if (!::history.isInitialized) return
        val prefs = getSharedPreferences("strength_history", MODE_PRIVATE)
        val records = JSONArray(prefs.getString("records", "[]"))
        val selected = exercise.selectedItem?.toString() ?: return
        val values = (0 until records.length()).mapNotNull { i ->
            records.optJSONObject(i)?.takeIf { it.optString("exercise") == selected }?.optDouble("value")
        }
        history.text = if (values.isEmpty()) "No saved sessions for this exercise yet."
            else "Personal best: " + String.format(Locale.UK, "%.1f kg", values.maxOrNull()) +
                "\\nLatest: " + String.format(Locale.UK, "%.1f kg", values.last()) +
                "\\nProgress since first: " + String.format(Locale.UK, "%+.1f kg", values.last() - values.first()) +
                "\\nSaved sessions: " + values.size
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
        getSharedPreferences("last_estimate", MODE_PRIVATE).edit().putString("label", "${exercise.selectedItem}: $label").apply()
        val manager = AppWidgetManager.getInstance(this)
        val component = ComponentName(this, OneRmWidget::class.java)
        val ids = manager.getAppWidgetIds(component)
        OneRmWidget().onUpdate(this, manager, ids)
    }
}
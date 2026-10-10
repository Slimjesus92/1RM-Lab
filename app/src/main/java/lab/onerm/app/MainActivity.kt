package lab.onerm.app

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.Typeface
import android.view.View
import android.text.InputType
import android.view.ViewGroup
import android.view.WindowManager
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
    private lateinit var rootView: LinearLayout
    private var darkMode = true
    private lateinit var dashboard: LinearLayout
    private lateinit var calculator: LinearLayout
    private lateinit var historyPage: LinearLayout
    private lateinit var settingsPage: LinearLayout
    private lateinit var dashboardCards: LinearLayout
    private lateinit var dashboardChart: ProgressChart
    private lateinit var navButtons: List<Button>
    private var historyDays = 0
    private fun filteredPoints(points: List<ProgressChart.Point>): List<ProgressChart.Point> {
        if (historyDays == 0) return points
        val cutoff = System.currentTimeMillis() - historyDays.toLong() * 86400000L
        return points.filter { it.timestamp >= cutoff }
    }
    private lateinit var weightInput: EditText
    private lateinit var repsInput: EditText
    private lateinit var rirInput: EditText
    private fun recallSet() {
        if (!::weightInput.isInitialized) return
        val name = exercise.selectedItem?.toString() ?: return
        val saved = getSharedPreferences("recent_sets", MODE_PRIVATE).getString(name, null)
        if (saved == null) {
            weightInput.text.clear()
            repsInput.text.clear()
            rirInput.text.clear()
            return
        }
        try {
            val obj = JSONObject(saved)
            weightInput.setText(obj.optString("weight"))
            repsInput.setText(obj.optString("reps"))
            rirInput.setText(obj.optString("rir"))
        } catch (_: Exception) {}
    }

    private var chartLift = "Bench press"
    private fun chosenLifts(): List<String> {
        val value = getSharedPreferences("ui_settings", MODE_PRIVATE)
            .getString("dashboard_lifts", "Bench press|Squat|Deadlift") ?: ""
        return value.split("|").filter { it in exerciseNames }.distinct().ifEmpty { listOf("Bench press") }
    }
    private fun chooseLifts() {
        val chosen = chosenLifts().toMutableSet()
        val flags = exerciseNames.map { it in chosen }.toBooleanArray()
        android.app.AlertDialog.Builder(this).setTitle("Dashboard exercises")
            .setMultiChoiceItems(exerciseNames.toTypedArray(), flags) { _, i, checked ->
                if (checked) chosen.add(exerciseNames[i]) else chosen.remove(exerciseNames[i])
            }
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                if (chosen.isEmpty()) {
                    Toast.makeText(this, "Choose at least one exercise", Toast.LENGTH_SHORT).show()
                } else {
                    getSharedPreferences("ui_settings", MODE_PRIVATE).edit()
                        .putString("dashboard_lifts", exerciseNames.filter { it in chosen }.joinToString("|")).apply()
                    renderDashboard()
                }
            }.show()
    }
    private val exerciseNames = listOf("Bench press", "Squat", "Deadlift", "Overhead press", "Barbell row", "Other")
    private fun rounded(color: Int, radius: Int = 18) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun title(label: String) = TextView(this).apply {
        text = label; textSize = 24f; setTypeface(null, Typeface.BOLD)
        setPadding(0, dp(8), 0, dp(20))
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun showTab(index: Int) {
        listOf(dashboard, calculator, historyPage, settingsPage).forEachIndexed { i, page ->
            page.visibility = if (i == index) View.VISIBLE else View.GONE
        }
        navButtons.forEachIndexed { i, button -> button.alpha = if (i == index) 1f else 0.6f }
    }
    private fun renderDashboard() {
        if (!::dashboardCards.isInitialized || !::exercise.isInitialized) return
        dashboardCards.removeAllViews()
        val all = records()
        for (name in chosenLifts()) {
            val entries = (0 until all.length()).mapNotNull { all.optJSONObject(it) }
                .filter { it.optString("exercise") == name }
            val estimated = entries.filter { it.optString("type", "estimated") != "actual" }
                .map { it.optDouble("value") }.filter { it.isFinite() && it > 0 }
            val tested = entries.filter { it.optString("type") == "actual" }
                .map { it.optDouble("value") }.filter { it.isFinite() && it > 0 }
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), dp(20), dp(20), dp(20))
                background = rounded(if (darkMode) Color.rgb(29, 40, 55) else Color.WHITE, 22)
                elevation = dp(3).toFloat()
            }
            box.addView(TextView(this).apply { text = name.uppercase(Locale.UK); textSize = 13f; setTextColor(if (darkMode) Color.WHITE else Color.rgb(27, 42, 60)) })
            box.addView(TextView(this).apply {
                text = if (estimated.isEmpty()) "—" else String.format(Locale.UK, "%.1f kg", estimated.maxOrNull())
                textSize = 32f; setTypeface(null, Typeface.BOLD)
                setTextColor(if (darkMode) Color.rgb(42, 184, 255) else Color.rgb(36, 101, 175))
            })
            box.addView(TextView(this).apply {
                text = if (tested.isEmpty()) "Estimated personal best" else
                    "Tested PB: " + String.format(Locale.UK, "%.1f kg", tested.maxOrNull())
                textSize = 12f; setTextColor(if (darkMode) Color.WHITE else Color.rgb(27, 42, 60))
            })
            box.addView(TextView(this).apply { text = progressLabel(estimated); textSize = 12f; setTextColor(if (darkMode) Color.WHITE else Color.rgb(27, 42, 60)) })
            box.setOnClickListener {
                exercise.setSelection(exerciseNames.indexOf(name))
                chartLift = name
                renderDashboard()
            }
            dashboardCards.addView(box, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        }
        val selected = chartLift
        dashboardChart.points = (0 until all.length()).mapNotNull { all.optJSONObject(it) }
            .filter { it.optString("exercise") == selected && it.optString("type", "estimated") != "actual" }
            .mapNotNull {
                val value = it.optDouble("value")
                if (value.isFinite() && value > 0) ProgressChart.Point(value, it.optLong("date", System.currentTimeMillis())) else null
            }
    }


    private fun progressLabel(values: List<Double>): String {
        if (values.size < 2) return "Log another session to see progress"
        val delta = values.last() - values.first()
        return when {
            delta > 0.5 -> "Improving: " + String.format(Locale.UK, "%+.1f kg", delta)
            delta < -0.5 -> "Below first estimate: " + String.format(Locale.UK, "%+.1f kg", delta)
            else -> "Holding steady"
        }
    }

    private fun trainingWeightTool(): LinearLayout {
        val section = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        section.addView(TextView(this).apply { text = "Training weights"; textSize = 20f })
        val picker = Spinner(this)
        picker.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, exerciseNames)
        section.addView(picker)
        val percentage = SeekBar(this).apply { max = 90; progress = 65 }
        section.addView(percentage)
        val output = TextView(this).apply { textSize = 18f; setPadding(0, dp(8), 0, dp(12)) }
        section.addView(output)
        val calculate = {
            val name = exerciseNames[picker.selectedItemPosition.coerceAtLeast(0)]
            val all = records()
            val values = (0 until all.length()).mapNotNull { all.optJSONObject(it) }
                .filter { it.optString("exercise") == name && it.optString("type", "estimated") != "actual" }
                .map { it.optDouble("value") }.filter { it.isFinite() && it > 0 }
            val percent = percentage.progress + 10
            output.text = if (values.isEmpty()) "$percent% — save an estimate first" else {
                val kg = kotlin.math.round(values.maxOrNull()!! * percent / 100.0 * 2.0) / 2.0
                "$percent% of estimated PB = " + String.format(Locale.UK, "%.1f kg", kg)
            }
        }
        percentage.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) { calculate() }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })
        picker.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) { calculate() }
        }
        calculate()
        return section
    }
    private fun applyTheme(view: View) {
        val foreground = if (darkMode) Color.WHITE else Color.rgb(27, 42, 60)
        val accent = if (darkMode) Color.rgb(42, 184, 255) else Color.rgb(36, 101, 175)
        when (view) {
            is Button -> {
                view.setTextColor(if (darkMode) Color.BLACK else Color.WHITE)
                view.backgroundTintList = android.content.res.ColorStateList.valueOf(accent)
            }
            is EditText -> {
                view.setTextColor(foreground)
                view.setHintTextColor(if (darkMode) Color.LTGRAY else Color.GRAY)
                view.backgroundTintList = android.content.res.ColorStateList.valueOf(accent)
            }
            is TextView -> view.setTextColor(foreground)
        }
        if (view is ViewGroup) for (i in 0 until view.childCount) applyTheme(view.getChildAt(i))
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        darkMode = getSharedPreferences("ui_settings", MODE_PRIVATE)
            .getString("theme", "Dark / Performance") != "Light / Scientific"
        window.statusBarColor = if (darkMode) Color.rgb(16, 23, 33) else Color.rgb(245, 248, 252)
        window.navigationBarColor = window.statusBarColor
        window.decorView.systemUiVisibility = if (darkMode) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        // Keep content above Android system navigation, including Samsung three-button navigation.
        window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(24))
            setBackgroundColor(if (darkMode) Color.rgb(16, 23, 33) else Color.rgb(245, 248, 252))
        }
        rootView = root
        val scroll = ScrollView(this).apply {
            addView(root)
            clipToPadding = false
            isFillViewport = true
            setBackgroundColor(if (darkMode) Color.rgb(16, 23, 33) else Color.rgb(245, 248, 252))
            val baseBottom = (28 * resources.displayMetrics.density).toInt()
            setPadding(0, 0, 0, baseBottom)
            setOnApplyWindowInsetsListener { view, insets ->
                val navigationBottom = insets.systemWindowInsetBottom
                view.setPadding(0, 0, 0, baseBottom + navigationBottom)
                insets
            }
        }
        val frame = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(if (darkMode) Color.rgb(16, 23, 33) else Color.rgb(245, 248, 252))
        }
        frame.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
            setBackgroundColor(if (darkMode) Color.rgb(29, 40, 55) else Color.WHITE)
        }
        navButtons = listOf("⌂ Home", "+ Log", "▤ History", "⚙ Settings").mapIndexed { index, label ->
            Button(this).apply {
                text = label; textSize = 11f; isAllCaps = false
                setOnClickListener { showTab(index) }
            }.also { nav.addView(it, LinearLayout.LayoutParams(0, dp(52), 1f)) }
        }
        frame.addView(nav)
        frame.setOnApplyWindowInsetsListener { _, insets ->
            root.setPadding(dp(18), dp(20) + insets.systemWindowInsetTop, dp(18), dp(24))
            nav.setPadding(dp(4), dp(4), dp(4), dp(4) + insets.systemWindowInsetBottom)
            insets
        }
        setContentView(frame)
        dashboard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        calculator = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        historyPage = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        settingsPage = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(dashboard); root.addView(calculator)
        root.addView(historyPage); root.addView(settingsPage)
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
        header.addView(title("Your strength"), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(Button(this).apply { text = "Edit"; isAllCaps = false; setOnClickListener { chooseLifts() } },
            LinearLayout.LayoutParams(dp(80), dp(48)))
        dashboard.addView(header)
        dashboardCards = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(18), 0, 0) }
        dashboard.addView(dashboardCards)
        dashboard.addView(TextView(this).apply { text = "YOUR PROGRESSION"; textSize = 18f })
        dashboardChart = ProgressChart(this).apply { darkMode = this@MainActivity.darkMode }
        dashboard.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(12), dp(8), dp(8))
            background = rounded(if (darkMode) Color.rgb(29, 40, 55) else Color.WHITE, 22)
            addView(dashboardChart)
        })
        calculator.addView(title("1RM Calculator"))
        calculator.addView(trainingWeightTool())
        historyPage.addView(title("Training history"))
        settingsPage.addView(title("Settings"))
        settingsPage.addView(TextView(this).apply { text = "DATA BACKUP"; textSize = 18f })
        settingsPage.addView(Button(this).apply { text = "Export history"; setOnClickListener { exportHistory() } })
        settingsPage.addView(Button(this).apply { text = "Import history"; setOnClickListener { importHistory() } })


        exercise = Spinner(this)
        val names = exerciseNames
        exercise.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        calculator.addView(TextView(this).apply { text = "SELECT LIFT"; textSize = 14f })
        calculator.addView(exercise)
        val historyExercise = Spinner(this)
        historyExercise.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        historyPage.addView(historyExercise)
        historyPage.addView(Button(this).apply { text = "Edit saved records"; setOnClickListener { chooseRecord() } })
        val dateFilter = Spinner(this)
        dateFilter.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            listOf("All time", "Last 90 days", "Last 30 days"))
        historyPage.addView(dateFilter)
        dateFilter.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                historyDays = when (position) { 1 -> 90; 2 -> 30; else -> 0 }
                showHistory()
            }
        }
        historyExercise.setSelection(0)
        historyExercise.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (exercise.selectedItemPosition != position) exercise.setSelection(position)
                showHistory()
            }
        }
        val weight = edit("Weight (kg)", true).also { weightInput = it }
        val reps = edit("Repetitions (1–15)", false).also { repsInput = it }
        val rir = edit("RIR (optional, 0–5)", false).also { rirInput = it }
        calculator.addView(weight); calculator.addView(reps); calculator.addView(rir)
        val add = Button(this).apply { text = "Add set" }
        calculator.addView(add)
        rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        calculator.addView(rows)
        result = TextView(this).apply { text = "Add a set to estimate your 1RM"; textSize = 22f; setPadding(0, 18, 0, 18) }
        calculator.addView(result)
        calculator.addView(Button(this).apply {
            text = "Save session"
            setOnClickListener {
                if (sets.isEmpty()) { Toast.makeText(this@MainActivity, "Add a set first", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                saveRecord(exercise.selectedItem.toString(), sets.maxOf { OneRmEngine.estimate(it).combinedKg }, "estimated")
                showHistory()
            }
        })
        history = TextView(this).apply { textSize = 17f; setPadding(0, 24, 0, 16) }
        historyPage.addView(history)
        chart = ProgressChart(this)
        historyPage.addView(chart)
        settingsPage.addView(TextView(this).apply { text = "WIDGET EXERCISE"; textSize = 18f })
        widgetExercise = Spinner(this)
        widgetExercise.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        settingsPage.addView(widgetExercise)
        val widgetPrefs = getSharedPreferences("last_estimate", MODE_PRIVATE)
        widgetExercise.setSelection(names.indexOf(widgetPrefs.getString("widget_exercise", names[0])).coerceAtLeast(0))
        widgetExercise.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                widgetPrefs.edit().putString("widget_exercise", names[position]).apply()
                updateWidget()
            }
        }
        calculator.addView(TextView(this).apply { text = "ACTUAL 1RM (TESTED LIFT)"; textSize = 18f; setPadding(0, 18, 0, 0) })
        val actualWeight = edit("Weight lifted for 1 rep (kg)", true)
        calculator.addView(actualWeight)
        calculator.addView(Button(this).apply {
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
                sets.clear(); refresh(); showHistory(); recallSet()
                if (historyExercise.selectedItemPosition != position) historyExercise.setSelection(position)
            }
        }
        showHistory()
        settingsPage.addView(TextView(this).apply { text = "Estimates are not measured 1RMs. Unknown RIR is provisionally treated as 0 for calculation, and should not be interpreted as confirmed failure. Set spread is not a statistical confidence interval." })
        add.setOnClickListener {
            try {
                val w = weight.text.toString().toDouble()
                val r = reps.text.toString().toInt()
                val reserve = rir.text.toString().takeIf { it.isNotBlank() }?.toInt()
                val set = LiftSet(w, r, reserve)
                OneRmEngine.estimate(set)
                sets.add(set)
                getSharedPreferences("recent_sets", MODE_PRIVATE).edit()
                    .putString(exercise.selectedItem.toString(), JSONObject()
                        .put("weight", w).put("reps", r).put("rir", reserve?.toString() ?: "").toString()).apply()
                refresh()
            } catch (e: Exception) {
                Toast.makeText(this, e.message ?: "Check inputs", Toast.LENGTH_LONG).show()
            }
        }
        calculator.addView(Button(this).apply { text = "Clear session"; setOnClickListener { sets.clear(); refresh() } })
        settingsPage.addView(TextView(this).apply { text = "APPEARANCE"; textSize = 20f; setPadding(0, 28, 0, 8) })
        val themes = listOf("Dark / Performance", "Light / Scientific")
        val themeSpinner = Spinner(this)
        themeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, themes)
        settingsPage.addView(themeSpinner)
        themeSpinner.setSelection(if (darkMode) 0 else 1)
        themeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val chosen = themes[position]
                val prefs = getSharedPreferences("ui_settings", MODE_PRIVATE)
                if (prefs.getString("theme", "Dark / Performance") != chosen) {
                    prefs.edit().putString("theme", chosen).apply()
                    recreate()
                }
            }
        }
        settingsPage.addView(TextView(this).apply { text = "1RM Lab v0.11 · Theme choice is saved automatically." })
        applyTheme(root)
        dashboardCards.let { container ->
            for (i in 0 until container.childCount) {
                val card = container.getChildAt(i)
                if (card is ViewGroup) {
                    for (j in 0 until card.childCount) {
                        (card.getChildAt(j) as? TextView)?.let { label ->
                            label.setTextColor(if (j == 1) (if (darkMode) Color.rgb(42, 184, 255) else Color.rgb(36, 101, 175)) else if (darkMode) Color.WHITE else Color.rgb(27, 42, 60))
                        }
                    }
                }
            }
        }
        applyTheme(nav)
        renderDashboard()
        showTab(0)
        chart.darkMode = darkMode
    }
    private fun records(): JSONArray {
        val raw = getSharedPreferences("strength_history", MODE_PRIVATE).getString("records", "[]") ?: "[]"
        return try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
    }
    private fun saveRecord(name: String, value: Double, type: String) {
        val all = records()
        val previous = (0 until all.length()).mapNotNull { all.optJSONObject(it) }
            .filter { it.optString("exercise") == name && it.optString("type", "estimated") == type }
            .map { it.optDouble("value") }.filter { it.isFinite() && it > 0 }.maxOrNull()
        all.put(JSONObject().put("exercise", name).put("value", value)
            .put("date", System.currentTimeMillis()).put("type", type))
        getSharedPreferences("strength_history", MODE_PRIVATE).edit().putString("records", all.toString()).apply()
        updateWidget()
        renderDashboard()
        if (previous == null || value > previous + 0.001) {
            val gain = if (previous == null) "First recorded PB" else String.format(Locale.UK, "+%.1f kg", value - previous)
            android.app.AlertDialog.Builder(this).setTitle("New personal best!")
                .setMessage(name + " · " + (if (type == "actual") "Tested" else "Estimated") +
                    " 1RM: " + String.format(Locale.UK, "%.1f kg", value) + " (" + gain + ")")
                .setPositiveButton("Nice!", null).show()
        }
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
        chart.points = filteredPoints(matching.filter { it.optString("type", "estimated") != "actual" }
            .mapNotNull {
                val value = it.optDouble("value")
                if (value.isFinite() && value > 0) ProgressChart.Point(value, it.optLong("date", System.currentTimeMillis())) else null
            })
        val format = { value: Double -> String.format(Locale.UK, "%.1f kg", value) }
        val estimateLabel = if (estimates.isEmpty()) "No estimated sessions saved"
            else "Estimated best: " + format(estimates.maxOrNull() ?: 0.0) +
                "\nLatest estimate: " + format(estimates.last()) +
                "\nChange: " + String.format(Locale.UK, "%+.1f kg", estimates.last() - estimates.first()) +
                "\nEstimated sessions: " + estimates.size
        val recentChange = if (estimates.size < 2) "Previous session: not enough data" else {
            val previous = estimates[estimates.lastIndex - 1]
            val delta = estimates.last() - previous
            "Vs previous estimated session: " + String.format(Locale.UK, "%+.1f kg (%+.1f%%)", delta, delta / previous * 100.0)
        }
        val testedChange = if (actuals.size < 2) "" else {
            val previous = actuals[actuals.lastIndex - 1]
            val delta = actuals.last() - previous
            " · Vs previous tested lift: " + String.format(Locale.UK, "%+.1f kg (%+.1f%%)", delta, delta / previous * 100.0)
        }
        val actualLabel = if (actuals.isEmpty()) "No tested 1RM saved"
            else "Tested 1RM personal best: " + format(actuals.maxOrNull() ?: 0.0)
        renderDashboard()
        history.text = "STRENGTH DASHBOARD — $selected\n\n$estimateLabel\n$recentChange\n\n$actualLabel$testedChange\n\nEstimated 1RM progression:"
    }


    private fun exportHistory() {
        val intent = android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(android.content.Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(android.content.Intent.EXTRA_TITLE, "1rm-lab-history.json")
        }
        startActivityForResult(intent, 1101)
    }
    private fun importHistory() {
        val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(android.content.Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, 1102)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            if (requestCode == 1101) {
                contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(JSONObject().put("format", "1rm-lab-history-v1")
                        .put("records", records()).toString(2).toByteArray(Charsets.UTF_8))
                } ?: error("Cannot write backup")
                Toast.makeText(this, "History exported", Toast.LENGTH_SHORT).show()
            } else if (requestCode == 1102) {
                val raw = contentResolver.openInputStream(uri)?.use { input ->
                    val bytes = input.readBytes()
                    require(bytes.size <= 5_000_000) { "Backup too large" }
                    bytes.toString(Charsets.UTF_8)
                } ?: error("Cannot read backup")
                val backup = JSONObject(raw)
                require(backup.optString("format") == "1rm-lab-history-v1") { "Not a 1RM Lab backup" }
                val incoming = backup.getJSONArray("records")
                require(incoming.length() <= 50000) { "Too many records" }
                for (i in 0 until incoming.length()) {
                    val entry = incoming.getJSONObject(i)
                    require(entry.optString("exercise") in exerciseNames) { "Unknown exercise" }
                    require(entry.optString("type", "estimated") in listOf("estimated", "actual")) { "Invalid type" }
                    val value = entry.optDouble("value")
                    require(value.isFinite() && value > 0 && entry.optLong("date") > 0) { "Invalid record" }
                }
                android.app.AlertDialog.Builder(this).setTitle("Replace saved history?")
                    .setMessage("Import " + incoming.length() + " records? Your existing history will be replaced. Export it first to keep a backup.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Replace") { _, _ -> persistRecords(incoming) }
                    .show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Backup error: " + (e.message ?: "Invalid file"), Toast.LENGTH_LONG).show()
        }
    }

    private fun persistRecords(all: JSONArray) {
        getSharedPreferences("strength_history", MODE_PRIVATE).edit().putString("records", all.toString()).apply()
        updateWidget()
        showHistory()
        renderDashboard()
    }
    private fun recordEditor(index: Int) {
        val all = records()
        val original = all.optJSONObject(index) ?: return
        val input = edit("1RM (kg)", true).apply { setText(original.optDouble("value").toString()) }
        val holder = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(input)
        }
        android.app.AlertDialog.Builder(this)
            .setTitle(original.optString("exercise") + " · " + original.optString("type", "estimated"))
            .setView(holder)
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ ->
                android.app.AlertDialog.Builder(this).setTitle("Delete this record?")
                    .setMessage("This cannot be undone. Export a backup first if needed.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        val revised = JSONArray()
                        for (i in 0 until all.length()) if (i != index) revised.put(all.get(i))
                        persistRecords(revised)
                    }.show()
            }
            .setPositiveButton("Save") { _, _ ->
                val value = input.text.toString().toDoubleOrNull()
                if (value == null || !value.isFinite() || value <= 0.0) {
                    Toast.makeText(this, "Enter a valid weight", Toast.LENGTH_LONG).show()
                } else {
                    original.put("value", value)
                    persistRecords(all)
                }
            }.show()
    }
    private fun chooseRecord() {
        val all = records()
        val name = exercise.selectedItem?.toString() ?: return
        val indices = (0 until all.length()).filter { all.optJSONObject(it)?.optString("exercise") == name }.reversed()
        if (indices.isEmpty()) {
            Toast.makeText(this, "No saved records for this lift", Toast.LENGTH_SHORT).show()
            return
        }
        val formatter = java.text.SimpleDateFormat("dd MMM yyyy", Locale.UK)
        val labels = indices.map { i ->
            val entry = all.getJSONObject(i)
            val kind = if (entry.optString("type") == "actual") "Tested" else "Estimated"
            "$kind · " + String.format(Locale.UK, "%.1f kg", entry.optDouble("value")) +
                " · " + formatter.format(java.util.Date(entry.optLong("date")))
        }
        android.app.AlertDialog.Builder(this).setTitle("Edit $name records")
            .setItems(labels.toTypedArray()) { _, which -> recordEditor(indices[which]) }
            .setNegativeButton("Cancel", null).show()
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
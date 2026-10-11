package lab.onerm.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View

/** Educational schematic: muscle involvement is not an EMG measurement. */
class MuscleMapView(context: Context) : View(context) {
    var exerciseName: String = "Bench press"
        set(value) { field = value; invalidate() }
    var dark: Boolean = true
        set(value) { field = value; invalidate() }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private data class Region(val key: String, val cx: Float, val cy: Float, val rx: Float, val ry: Float)
    private val front = listOf(
        Region("delts", 0.20f, .23f, .09f, .09f), Region("delts", .80f, .23f, .09f, .09f),
        Region("chest", .39f, .28f, .12f, .09f), Region("chest", .61f, .28f, .12f, .09f),
        Region("biceps", .16f, .40f, .06f, .12f), Region("biceps", .84f, .40f, .06f, .12f),
        Region("core", .50f, .48f, .15f, .14f),
        Region("quads", .39f, .73f, .09f, .17f), Region("quads", .61f, .73f, .09f, .17f))
    private val back = listOf(
        Region("rear delts", .20f, .23f, .09f, .09f), Region("rear delts", .80f, .23f, .09f, .09f),
        Region("upper back", .50f, .30f, .22f, .11f),
        Region("lats", .34f, .44f, .10f, .13f), Region("lats", .66f, .44f, .10f, .13f),
        Region("erectors", .50f, .51f, .07f, .17f),
        Region("triceps", .16f, .40f, .06f, .12f), Region("triceps", .84f, .40f, .06f, .12f),
        Region("glutes", .40f, .64f, .10f, .10f), Region("glutes", .60f, .64f, .10f, .10f),
        Region("hamstrings", .39f, .78f, .09f, .13f), Region("hamstrings", .61f, .78f, .09f, .13f))
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, (width * .73f).toInt().coerceAtLeast(200))
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val (primary, secondary) = muscleGroups(exerciseName)
        val w = width.toFloat()
        val h = height.toFloat()
        val panelWidth = w / 2f
        for (side in 0..1) {
            val left = side * panelWidth
            val bodyW = panelWidth * .72f
            val bodyH = h * .85f
            val x0 = left + (panelWidth - bodyW) / 2
            val y0 = h * .10f
            paint.style = Paint.Style.FILL
            paint.color = if (dark) Color.rgb(58, 71, 83) else Color.rgb(211, 221, 228)
            canvas.drawOval(x0 + bodyW * .39f, y0, x0 + bodyW * .61f, y0 + bodyH * .18f, paint)
            canvas.drawRoundRect(x0 + bodyW * .27f, y0 + bodyH * .18f,
                x0 + bodyW * .73f, y0 + bodyH * .65f, 25f, 25f, paint)
            canvas.drawRoundRect(x0 + bodyW * .09f, y0 + bodyH * .20f,
                x0 + bodyW * .25f, y0 + bodyH * .61f, 16f, 16f, paint)
            canvas.drawRoundRect(x0 + bodyW * .75f, y0 + bodyH * .20f,
                x0 + bodyW * .91f, y0 + bodyH * .61f, 16f, 16f, paint)
            canvas.drawRoundRect(x0 + bodyW * .29f, y0 + bodyH * .59f,
                x0 + bodyW * .47f, y0 + bodyH, 13f, 13f, paint)
            canvas.drawRoundRect(x0 + bodyW * .53f, y0 + bodyH * .59f,
                x0 + bodyW * .71f, y0 + bodyH, 13f, 13f, paint)
            for (region in if (side == 0) front else back) {
                paint.color = when (region.key) {
                    in primary -> Color.rgb(45, 137, 191)
                    in secondary -> Color.rgb(132, 185, 207)
                    else -> if (dark) Color.rgb(79, 93, 105) else Color.rgb(186, 199, 208)
                }
                canvas.drawOval(x0 + bodyW * (region.cx - region.rx),
                    y0 + bodyH * (region.cy - region.ry),
                    x0 + bodyW * (region.cx + region.rx),
                    y0 + bodyH * (region.cy + region.ry), paint)
            }
            paint.color = if (dark) Color.WHITE else Color.rgb(35, 47, 60)
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 13f * resources.displayMetrics.scaledDensity
            canvas.drawText(if (side == 0) "FRONT" else "BACK",
                left + panelWidth / 2, h * .075f, paint)
        }
    }
    companion object {
        fun muscleGroups(exercise: String): Pair<Set<String>, Set<String>> = when (exercise) {
            "Bench press" -> setOf("chest") to setOf("triceps", "delts")
            "Squat" -> setOf("quads", "glutes") to setOf("hamstrings", "erectors", "core")
            "Deadlift" -> setOf("glutes", "hamstrings") to setOf("quads", "erectors", "lats", "upper back", "core")
            "Overhead press" -> setOf("delts") to setOf("triceps", "core", "upper back")
            "Barbell row" -> setOf("lats", "upper back") to setOf("rear delts", "biceps", "erectors")
            else -> emptySet<String>() to emptySet()
        }
        fun description(exercise: String): String {
            val (primary, secondary) = muscleGroups(exercise)
            if (primary.isEmpty()) return "No specific muscle classification for this exercise."
            fun label(s: String) = s.replaceFirstChar { it.uppercase() }
            return "Primary: " + primary.joinToString(", ") { label(it) } +
                "\nSecondary / stabilising: " + secondary.joinToString(", ") { label(it) } +
                "\n\nIllustrative muscle involvement; emphasis varies with technique and individual anatomy."
        }
    }
}

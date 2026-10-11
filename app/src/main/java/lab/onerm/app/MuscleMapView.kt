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
    private data class Region(val key: String, val x: Float, val y: Float, val rx: Float, val ry: Float, val tilt: Float = 0f)
    private val front = listOf(
        Region("neck", .50f,.17f,.085f,.05f),
        Region("delts", .23f,.27f,.105f,.087f,-24f), Region("delts",.77f,.27f,.105f,.087f,24f),
        Region("chest", .39f,.31f,.125f,.09f,-14f), Region("chest",.61f,.31f,.125f,.09f,14f),
        Region("biceps",.155f,.405f,.060f,.11f,-9f), Region("biceps",.845f,.405f,.060f,.11f,9f),
        Region("forearms",.105f,.565f,.048f,.125f,-8f), Region("forearms",.895f,.565f,.048f,.125f,8f),
        Region("core",.50f,.475f,.125f,.13f),
        Region("obliques",.34f,.49f,.07f,.11f), Region("obliques",.66f,.49f,.07f,.11f),
        Region("adductors",.455f,.70f,.045f,.14f,8f), Region("adductors",.545f,.70f,.045f,.14f,-8f),
        Region("quads",.355f,.73f,.09f,.17f,-6f), Region("quads",.645f,.73f,.09f,.17f,6f),
        Region("calves",.365f,.92f,.065f,.07f), Region("calves",.635f,.92f,.065f,.07f))
    private val back = listOf(
        Region("traps",.50f,.245f,.16f,.085f),
        Region("rear delts",.23f,.27f,.10f,.08f,-24f), Region("rear delts",.77f,.27f,.10f,.08f,24f),
        Region("upper back",.50f,.35f,.155f,.10f),
        Region("lats",.345f,.445f,.10f,.125f,12f), Region("lats",.655f,.445f,.10f,.125f,-12f),
        Region("triceps",.155f,.405f,.06f,.115f,-9f), Region("triceps",.845f,.405f,.06f,.115f,9f),
        Region("forearms",.105f,.565f,.048f,.125f,-8f), Region("forearms",.895f,.565f,.048f,.125f,8f),
        Region("erectors",.46f,.51f,.042f,.145f), Region("erectors",.54f,.51f,.042f,.145f),
        Region("glutes",.405f,.655f,.10f,.10f), Region("glutes",.595f,.655f,.10f,.10f),
        Region("hamstrings",.355f,.79f,.082f,.135f,-5f), Region("hamstrings",.645f,.79f,.082f,.135f,5f),
        Region("calves",.365f,.92f,.065f,.07f), Region("calves",.635f,.92f,.065f,.07f))
    /** Muscle-specific tapered paths replace the previous generic oval markers. */
    private fun drawRegion(canvas: Canvas, region: Region, x: Float, y: Float, w: Float, h: Float, color: Int) {
        val cx = x + w * region.x
        val cy = y + h * region.y
        val rx = w * region.rx
        val ry = h * region.ry
        canvas.save()
        canvas.rotate(region.tilt, cx, cy)
        val path = Path()
        when (region.key) {
            "chest" -> {
                path.moveTo(cx - rx, cy - ry * .75f)
                path.cubicTo(cx - rx * .3f, cy - ry * 1.2f, cx + rx * .9f, cy - ry * .8f, cx + rx, cy)
                path.cubicTo(cx + rx * .55f, cy + ry * .8f, cx - rx * .4f, cy + ry, cx - rx, cy + ry * .25f)
            }
            "delts", "rear delts" -> {
                path.moveTo(cx - rx * .95f, cy - ry * .4f)
                path.cubicTo(cx - rx * .4f, cy - ry * 1.2f, cx + rx * .75f, cy - ry, cx + rx, cy - ry * .15f)
                path.cubicTo(cx + rx * .55f, cy + ry * .5f, cx + rx * .15f, cy + ry, cx - rx * .35f, cy + ry)
                path.close()
            }
            "quads", "hamstrings", "biceps", "triceps", "forearms", "calves", "adductors", "erectors" -> {
                path.moveTo(cx - rx * .75f, cy - ry)
                path.cubicTo(cx - rx * 1.25f, cy - ry * .35f, cx - rx * .95f, cy + ry * .4f, cx - rx * .3f, cy + ry)
                path.cubicTo(cx + rx * .35f, cy + ry * 1.05f, cx + rx, cy + ry * .25f, cx + rx * .8f, cy - ry * .65f)
                path.quadTo(cx, cy - ry * 1.15f, cx - rx * .75f, cy - ry)
            }
            "lats", "traps", "upper back" -> {
                path.moveTo(cx - rx, cy - ry)
                path.quadTo(cx, cy - ry * .65f, cx + rx, cy - ry)
                path.cubicTo(cx + rx * .8f, cy, cx + rx * .4f, cy + ry * .8f, cx, cy + ry)
                path.cubicTo(cx - rx * .4f, cy + ry * .8f, cx - rx * .8f, cy, cx - rx, cy - ry)
            }
            "glutes" -> {
                path.moveTo(cx - rx, cy - ry * .65f)
                path.cubicTo(cx - rx * .3f, cy - ry * 1.1f, cx + rx * .85f, cy - ry * .8f, cx + rx, cy)
                path.cubicTo(cx + rx * .85f, cy + ry * 1.1f, cx - rx * .45f, cy + ry * 1.2f, cx - rx, cy + ry * .4f)
            }
            "core" -> {
                path.moveTo(cx - rx, cy - ry)
                path.lineTo(cx + rx, cy - ry)
                path.lineTo(cx + rx * .75f, cy + ry)
                path.lineTo(cx - rx * .75f, cy + ry)
            }
            else -> path.addOval(cx - rx, cy - ry, cx + rx, cy + ry, Path.Direction.CW)
        }
        path.close()
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = resources.displayMetrics.density * 1.15f
        paint.color = if (dark) Color.rgb(24, 36, 48) else Color.rgb(122, 142, 154)
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        canvas.restore()
    }
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
                val color = when (region.key) {
                    in primary -> Color.rgb(45, 137, 191)
                    in secondary -> Color.rgb(132, 185, 207)
                    else -> if (dark) Color.rgb(79, 93, 105) else Color.rgb(186, 199, 208)
                }
                drawRegion(canvas, region, x0, y0, bodyW, bodyH, color)
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
            "Squat" -> setOf("quads", "glutes") to setOf("adductors", "erectors", "core")
            "Deadlift" -> setOf("glutes", "hamstrings") to setOf("quads", "adductors", "erectors", "lats", "traps", "core")
            "Overhead press" -> setOf("delts") to setOf("triceps", "traps", "core")
            "Barbell row" -> setOf("lats", "upper back") to setOf("rear delts", "biceps", "traps", "erectors")
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

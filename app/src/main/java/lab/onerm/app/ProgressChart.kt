package lab.onerm.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.view.MotionEvent
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class ProgressChart(context: Context) : View(context) {
    data class Point(val value: Double, val timestamp: Long)
    var darkMode: Boolean = false
        set(value) { field = value; invalidate() }
    var points: List<Point> = emptyList()
        set(value) { field = value; selectedIndex = -1; invalidate() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var hitPositions: List<Pair<Float, Float>> = emptyList()
    private var selectedIndex = -1
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && hitPositions.isNotEmpty()) {
            val i = hitPositions.indices.minByOrNull { kotlin.math.abs(hitPositions[it].first - event.x) } ?: return true
            selectedIndex = i
            invalidate()
            val point = points[i]
            Toast.makeText(context, String.format(Locale.UK, "%.1f kg · %s", point.value,
                SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK).format(Date(point.timestamp))),
                Toast.LENGTH_LONG).show()
            performClick()
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
    private val accent get() = if (darkMode) Color.rgb(42, 184, 255) else Color.rgb(30, 110, 195)
    private val labelColor get() = if (darkMode) Color.WHITE else Color.DKGRAY
    private val gridColor get() = if (darkMode) Color.rgb(70, 84, 100) else Color.LTGRAY
    private val density get() = resources.displayMetrics.density
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (240 * density).toInt())
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = 64f * density
        val right = width - 18f * density
        val top = 30f * density
        val bottom = height - 54f * density
        if (right <= left || bottom <= top) return
        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.textSize = 11f * resources.displayMetrics.scaledDensity
        paint.strokeWidth = density
        paint.color = labelColor
        canvas.drawText("Estimated 1RM (kg)", left, 18f * density, paint)
        val values = points.map { it.value }
        val min = values.minOrNull() ?: 0.0
        val high = values.maxOrNull() ?: 5.0
        val span = max(5.0, high - min)
        val floor = if (values.isEmpty()) 0.0 else min - (span - (high - min)) / 2.0
        for (i in 0..4) {
            val y = top + (bottom - top) * i / 4f
            paint.color = gridColor
            canvas.drawLine(left, y, right, y, paint)
            paint.color = labelColor
            val label = String.format(Locale.UK, "%.1f", floor + span * (4 - i) / 4.0)
            canvas.drawText(label, left - paint.measureText(label) - 7f * density, y + 4f * density, paint)
        }
        if (points.isEmpty()) {
            canvas.drawText("Save sessions to see progression", left, (top + bottom) / 2, paint)
            return
        }
        val firstTime = points.minOf { it.timestamp }
        val lastTime = points.maxOf { it.timestamp }
        val positions = points.mapIndexed { i, point ->
            val fraction = if (lastTime > firstTime) (point.timestamp - firstTime).toDouble() / (lastTime - firstTime)
                else if (points.size > 1) i.toDouble() / (points.size - 1) else 0.5
            val x = left + (right - left) * fraction.toFloat()
            val y = bottom - ((point.value - floor) / span).toFloat() * (bottom - top)
            Pair(x, y)
        }
        hitPositions = positions
        paint.color = accent
        paint.strokeWidth = 2.5f * density
        for (i in 1 until positions.size) {
            canvas.drawLine(positions[i - 1].first, positions[i - 1].second, positions[i].first, positions[i].second, paint)
        }
        for ((i, pos) in positions.withIndex()) canvas.drawCircle(pos.first, pos.second, (if (i == selectedIndex) 7f else 3.5f) * density, paint)
        paint.color = labelColor
        paint.textSize = 10f * resources.displayMetrics.scaledDensity
        val dateFormat = SimpleDateFormat(if (lastTime - firstTime < 86400000L) "dd MMM HH:mm" else "dd MMM", Locale.UK)
        val ticks = if (lastTime - firstTime >= 86400000L) listOf(0.0, 0.5, 1.0) else listOf(0.5)
        for (fraction in ticks) {
            val stamp = firstTime + ((lastTime - firstTime) * fraction).toLong()
            val label = dateFormat.format(Date(stamp))
            val x = left + (right - left) * fraction.toFloat()
            canvas.drawText(label, (x - paint.measureText(label) / 2).coerceIn(0f, width - paint.measureText(label)), bottom + 18f * density, paint)
        }
        val axisLabel = "Session date"
        canvas.drawText(axisLabel, (left + right - paint.measureText(axisLabel)) / 2, height - 8f * density, paint)
        val bestIndex = values.indices.maxByOrNull { values[it] } ?: return
        val (bestX, bestY) = positions[bestIndex]
        val bestLabel = String.format(Locale.UK, "PB %.1f kg", values[bestIndex])
        paint.color = accent
        canvas.drawText(bestLabel, (bestX - paint.measureText(bestLabel) / 2).coerceIn(left, right - paint.measureText(bestLabel)), (bestY - 9f * density).coerceAtLeast(top - 5f * density), paint)
    }
}

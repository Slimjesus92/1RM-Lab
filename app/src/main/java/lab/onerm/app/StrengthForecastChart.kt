package lab.onerm.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/** Observed session-best estimates and a clearly separated exploratory forecast. */
class StrengthForecastChart(context: Context) : View(context) {
    var darkMode = true
        set(value) { field = value; invalidate() }
    var observations: List<Pair<Long, Double>> = emptyList()
        set(value) { field = value; invalidate() }
    var forecast: StrengthForecast.Forecast? = null
        set(value) { field = value; invalidate() }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density get() = resources.displayMetrics.density

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (260 * density).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val sorted = observations.sortedBy { it.first }
        if (sorted.isEmpty()) return
        val left = 55f * density
        val right = width - 16f * density
        val top = 32f * density
        val bottom = height - 46f * density
        if (right <= left || bottom <= top) return
        val textColor = if (darkMode) Color.rgb(216, 226, 235) else Color.rgb(47, 62, 76)
        val gridColor = if (darkMode) Color.rgb(61, 74, 85) else Color.rgb(219, 227, 233)
        val observedColor = Color.rgb(45, 137, 191)
        val projectedColor = Color.rgb(216, 169, 79)
        val first = sorted.first().first
        val last = sorted.last().first
        val future = if (forecast == null) last else last + 28L * 86400000L
        val allValues = sorted.map { it.second } +
            (forecast?.let { listOf(it.low, it.high) } ?: emptyList())
        val low = allValues.minOrNull() ?: 0.0
        val high = allValues.maxOrNull() ?: 1.0
        val padding = max(3.0, (high - low) * 0.12)
        val floor = max(0.0, low - padding)
        val ceiling = high + padding
        fun x(stamp: Long): Float = left + ((stamp - first).toDouble() /
            (future - first).coerceAtLeast(1L)).toFloat() * (right - left)
        fun y(value: Double): Float = bottom - ((value - floor) /
            (ceiling - floor)).toFloat() * (bottom - top)
        paint.textSize = 11f * resources.displayMetrics.scaledDensity
        paint.style = Paint.Style.FILL
        paint.color = textColor
        canvas.drawText("Estimated 1RM (kg)", left, 19f * density, paint)
        for (i in 0..4) {
            val level = floor + (ceiling - floor) * i / 4.0
            val yy = y(level)
            paint.color = gridColor
            paint.strokeWidth = density
            canvas.drawLine(left, yy, right, yy, paint)
            paint.color = textColor
            val label = String.format(Locale.UK, "%.0f", level)
            canvas.drawText(label, left - paint.measureText(label) - 7f * density, yy + 4f * density, paint)
        }
        paint.color = observedColor
        paint.strokeWidth = 2.5f * density
        paint.strokeCap = Paint.Cap.ROUND
        for (i in 1 until sorted.size) {
            canvas.drawLine(x(sorted[i - 1].first), y(sorted[i - 1].second),
                x(sorted[i].first), y(sorted[i].second), paint)
        }
        for ((stamp, value) in sorted) canvas.drawCircle(x(stamp), y(value), 3.5f * density, paint)
        val result = forecast
        if (result != null) {
            val xx = x(future)
            val startX = x(last)
            val startY = y(sorted.last().second)
            val upper = y(result.high)
            val lower = y(result.low)
            val band = Path().apply {
                moveTo(startX, startY)
                lineTo(xx, upper)
                lineTo(xx, lower)
                close()
            }
            paint.color = Color.argb(58, 216, 169, 79)
            paint.style = Paint.Style.FILL
            canvas.drawPath(band, paint)
            paint.color = projectedColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f * density
            paint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(7f * density, 5f * density), 0f)
            canvas.drawLine(startX, startY, xx, y(result.projected), paint)
            paint.pathEffect = null
            paint.style = Paint.Style.FILL
            canvas.drawCircle(xx, y(result.projected), 4f * density, paint)
        }
        paint.color = textColor
        paint.textSize = 10f * resources.displayMetrics.scaledDensity
        val format = SimpleDateFormat("dd MMM", Locale.UK)
        canvas.drawText(format.format(Date(first)), left, height - 23f * density, paint)
        val lastLabel = format.format(Date(future))
        canvas.drawText(lastLabel, right - paint.measureText(lastLabel), height - 23f * density, paint)
        paint.color = observedColor
        canvas.drawText("Solid: recorded estimates", left, height - 7f * density, paint)
        if (result != null) {
            paint.color = projectedColor
            val label = "Dashed: forecast"
            canvas.drawText(label, right - paint.measureText(label), 19f * density, paint)
        }
    }
}

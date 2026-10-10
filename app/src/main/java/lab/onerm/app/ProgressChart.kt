package lab.onerm.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import kotlin.math.max

class ProgressChart(context: Context) : View(context) {
    var values: List<Double> = emptyList()
        set(value) { field = value; invalidate() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val accent = Color.rgb(30, 130, 220)
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (180 * resources.displayMetrics.density).toInt())
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pad = 22f * resources.displayMetrics.density
        val left = pad
        val right = width - pad
        val top = pad
        val bottom = height - pad
        paint.strokeWidth = 1f * resources.displayMetrics.density
        paint.color = Color.LTGRAY
        for (i in 0..3) {
            val y = top + (bottom - top) * i / 3f
            canvas.drawLine(left, y, right, y, paint)
        }
        if (values.isEmpty()) return
        val min = values.minOrNull() ?: return
        val maxValue = values.maxOrNull() ?: return
        val span = max(5.0, maxValue - min)
        val floor = min - (span - (maxValue - min)) / 2.0
        val coordinates = values.mapIndexed { i, v ->
            val x = if (values.size == 1) (left + right) / 2 else left + (right - left) * i / (values.size - 1)
            val y = bottom - ((v - floor) / span).toFloat() * (bottom - top)
            Pair(x, y)
        }
        paint.color = accent
        paint.strokeWidth = 3f * resources.displayMetrics.density
        for (i in 1 until coordinates.size) {
            canvas.drawLine(coordinates[i-1].first, coordinates[i-1].second, coordinates[i].first, coordinates[i].second, paint)
        }
        for ((x, y) in coordinates) canvas.drawCircle(x, y, 4f * resources.displayMetrics.density, paint)
    }
}

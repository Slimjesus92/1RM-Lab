package lab.onerm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StrengthTrendTest {
    private fun series(count: Int, step: Double = 1.0): List<StrengthTrend.Observation> =
        (0 until count).map { StrengthTrend.Observation(it * 7.0, 100.0 + it * step) }

    @Test fun insufficientHistoryDoesNotPredict() {
        assertNull(StrengthTrend.project(series(3)))
        assertNull(StrengthTrend.project((0..3).map { StrengthTrend.Observation(it.toDouble(), 100.0) }))
    }

    @Test fun flatPerformanceForecastIsFlat() {
        val p = assertNotNull(StrengthTrend.project(series(7, 0.0)))
        assertEquals(100.0, p.projected, 0.0001)
        assertTrue(p.low < p.projected && p.high > p.projected)
    }

    @Test fun steadyProgressProjectsUp() {
        val p = assertNotNull(StrengthTrend.project(series(8, 1.0)))
        assertTrue(p.projected > p.current)
        assertTrue(p.projected <= p.current * 1.15)
    }

    @Test fun noisyResultsWidenBand() {
        val smooth = assertNotNull(StrengthTrend.project(series(8, 1.0)))
        val noisy = assertNotNull(StrengthTrend.project((0 until 8).map {
            StrengthTrend.Observation(it * 7.0, 100.0 + it + if (it % 2 == 0) 12.0 else -12.0)
        }))
        assertTrue(noisy.high - noisy.low > smooth.high - smooth.low)
    }

    @Test fun backtestUsesOnlyPastObservations() {
        val result = assertNotNull(StrengthTrend.backtest(series(16)))
        assertTrue(result.samples > 0)
        assertTrue(result.meanAbsoluteErrorKg >= 0.0)
        assertTrue(result.withinBand in 0..result.samples)
    }

    @Test fun shortHistoryCannotBacktest() {
        assertNull(StrengthTrend.backtest(series(4)))
    }
}

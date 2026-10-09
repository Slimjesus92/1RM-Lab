package lab.onerm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OneRmEngineTest {
    @Test fun epleyFiveReps() { assertEquals(116.66666666666667, OneRmEngine.estimate(LiftSet(100.0, 5)).epleyKg, 0.00001) }
    @Test fun brzyckiFiveReps() { assertEquals(112.5, OneRmEngine.estimate(LiftSet(100.0, 5)).brzyckiKg, 0.00001) }
    @Test fun oneRep() { assertEquals(100.0, OneRmEngine.estimate(LiftSet(100.0, 1, 0)).combinedKg) }
    @Test fun rirIncreasesPrediction() { assertTrue(OneRmEngine.estimate(LiftSet(100.0, 5, 2)).combinedKg > OneRmEngine.estimate(LiftSet(100.0, 5, 0)).combinedKg) }
    @Test fun medianResistsOutlier() { assertEquals(OneRmEngine.estimate(LiftSet(100.0, 5)).combinedKg, OneRmEngine.session(listOf(LiftSet(100.0, 5), LiftSet(100.0, 5), LiftSet(50.0, 5))).oneRmKg) }
    @Test fun emptySessionRejected() { assertFailsWith<IllegalArgumentException> { OneRmEngine.session(emptyList()) } }
    @Test fun invalidWeightRejected() { assertFailsWith<IllegalArgumentException> { OneRmEngine.estimate(LiftSet(-1.0, 5)) } }
    @Test fun invalidRepsRejected() { assertFailsWith<IllegalArgumentException> { OneRmEngine.estimate(LiftSet(100.0, 0)) } }
}
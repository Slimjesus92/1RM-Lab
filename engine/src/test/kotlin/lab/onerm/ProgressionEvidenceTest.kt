package lab.onerm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class ProgressionEvidenceTest {
    private fun set(kg: Double, reps: Int = 8, rir: Int? = 3) =
        ProgressionEvidence.Set(kg, reps, rir)

    @Test fun toleratedSmallIncrease() {
        val outcome = assertNotNull(ProgressionEvidence.compare(set(60.0), set(62.5, 7, 2)))
        assertEquals(2.5, outcome.incrementKg)
        assertEquals(true, outcome.tolerated)
    }

    @Test fun grindingIncreaseIsNotTolerated() {
        assertEquals(false, assertNotNull(ProgressionEvidence.compare(set(60.0), set(62.5, 7, 0))).tolerated)
    }

    @Test fun differentRepTargetsAreNotComparable() {
        assertNull(ProgressionEvidence.compare(set(60.0, 10), set(70.0, 3)))
    }

    @Test fun missingRirIsNotEvidence() {
        assertNull(ProgressionEvidence.compare(set(60.0, 8, null), set(62.5)))
        assertNull(ProgressionEvidence.compare(set(60.0), set(62.5, 8, null)))
    }

    @Test fun dramaticallyDifferentEffortIsNotComparable() {
        assertNull(ProgressionEvidence.compare(set(60.0, 8, 7), set(62.5, 8, 2)))
    }

    @Test fun successHistorySupportsIncrement() {
        val o = listOf(
            ProgressionEvidence.Outcome(2.5, true),
            ProgressionEvidence.Outcome(2.5, true),
            ProgressionEvidence.Outcome(5.0, false)
        )
        assertEquals(2.5, ProgressionEvidence.selectStep(o, 5.0))
    }

    @Test fun failureAtSameStepBlocksRecommendation() {
        val o = listOf(
            ProgressionEvidence.Outcome(2.5, true),
            ProgressionEvidence.Outcome(2.5, true),
            ProgressionEvidence.Outcome(2.5, false)
        )
        assertNull(ProgressionEvidence.selectStep(o, 5.0))
    }

    @Test fun insufficientEvidenceDoesNotLearn() {
        assertNull(ProgressionEvidence.selectStep(listOf(ProgressionEvidence.Outcome(2.5, true)), 5.0))
    }
}

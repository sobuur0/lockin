package com.lockin.domain.statistics

import com.lockin.data.dao.ApplicationBlockCount
import com.lockin.data.dao.MoodUsageCount
import com.lockin.data.dao.StatisticsTotals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsCalculatorTest {
    @Test
    fun returnsEmptySummaryForNoHistory() {
        val summary = StatisticsCalculator.calculate(
            totals = StatisticsTotals(
                totalLockDuration = null,
                completedLockSessionCount = 0,
                longestLockDuration = null,
                averageLockDuration = null
            ),
            mostBlockedApplications = emptyList(),
            uniqueBlockedApplicationCount = 0,
            mostFrequentlyUsedMood = null
        )

        assertTrue(summary.isEmpty)
        assertEquals("0s", summary.totalDurationLabel)
        assertEquals(0, summary.completedLockSessionCount)
    }

    @Test
    fun calculatesLabelsAndRankingsFromLocalHistory() {
        val summary = StatisticsCalculator.calculate(
            totals = StatisticsTotals(
                totalLockDuration = 7_200_000L,
                completedLockSessionCount = 2,
                longestLockDuration = 5_400_000L,
                averageLockDuration = 3_600_000.0
            ),
            mostBlockedApplications = listOf(
                ApplicationBlockCount(
                    packageId = "com.example.one",
                    displayName = "One",
                    blockCount = 3
                )
            ),
            uniqueBlockedApplicationCount = 1,
            mostFrequentlyUsedMood = MoodUsageCount(
                moodId = 1,
                name = "Study",
                useCount = 2
            )
        )

        assertEquals("2h 0m", summary.totalDurationLabel)
        assertEquals("1h 30m", summary.longestDurationLabel)
        assertEquals("1h 0m", summary.averageDurationLabel)
        assertEquals("Study", summary.mostFrequentlyUsedMoodName)
        assertEquals("One", summary.mostBlockedApplications.single().displayName)
    }
}

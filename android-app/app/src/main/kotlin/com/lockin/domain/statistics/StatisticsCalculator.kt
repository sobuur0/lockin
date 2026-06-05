package com.lockin.domain.statistics

import com.lockin.data.dao.ApplicationBlockCount
import com.lockin.data.dao.MoodUsageCount
import com.lockin.data.dao.StatisticsTotals

data class BlockedAppSummary(
    val packageId: String,
    val displayName: String,
    val blockCount: Int
)

data class StatisticSummary(
    val totalDurationMillis: Long,
    val totalDurationLabel: String,
    val completedLockSessionCount: Int,
    val longestDurationMillis: Long,
    val longestDurationLabel: String,
    val averageDurationMillis: Long,
    val averageDurationLabel: String,
    val uniqueBlockedApplicationCount: Int,
    val mostBlockedApplications: List<BlockedAppSummary>,
    val mostFrequentlyUsedMoodName: String?
) {
    val isEmpty: Boolean = completedLockSessionCount == 0
}

object StatisticsCalculator {
    fun calculate(
        totals: StatisticsTotals,
        mostBlockedApplications: List<ApplicationBlockCount>,
        uniqueBlockedApplicationCount: Int,
        mostFrequentlyUsedMood: MoodUsageCount?
    ): StatisticSummary {
        val totalDuration = totals.totalLockDuration ?: 0L
        val longestDuration = totals.longestLockDuration ?: 0L
        val averageDuration = totals.averageLockDuration?.toLong() ?: 0L
        return StatisticSummary(
            totalDurationMillis = totalDuration,
            totalDurationLabel = durationLabel(totalDuration),
            completedLockSessionCount = totals.completedLockSessionCount,
            longestDurationMillis = longestDuration,
            longestDurationLabel = durationLabel(longestDuration),
            averageDurationMillis = averageDuration,
            averageDurationLabel = durationLabel(averageDuration),
            uniqueBlockedApplicationCount = uniqueBlockedApplicationCount,
            mostBlockedApplications = mostBlockedApplications.map { app ->
                BlockedAppSummary(
                    packageId = app.packageId,
                    displayName = app.displayName,
                    blockCount = app.blockCount
                )
            },
            mostFrequentlyUsedMoodName = mostFrequentlyUsedMood?.name
        )
    }

    fun durationLabel(durationMillis: Long): String {
        val seconds = (durationMillis / 1_000L).coerceAtLeast(0)
        val days = seconds / 86_400L
        val hours = (seconds % 86_400L) / 3_600L
        val minutes = (seconds % 3_600L) / 60L

        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }
}

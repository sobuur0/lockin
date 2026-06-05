package com.lockin.ui.stats

import com.lockin.data.dao.ApplicationBlockCount
import com.lockin.data.dao.MoodUsageCount
import com.lockin.data.entities.LockSessionApplicationEntity
import com.lockin.data.entities.LockSessionEntity
import com.lockin.data.entities.LockSourceType
import com.lockin.testsupport.FakeStatisticsRepository
import com.lockin.testsupport.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StatsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun showsEmptyStateForNoCompletedHistory() = runTest {
        val viewModel = StatsViewModel(FakeStatisticsRepository())
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.summary.isEmpty)
        assertEquals(0, viewModel.uiState.value.summary.completedLockSessionCount)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun summarizesLocalCompletedLockHistory() = runTest {
        val repository = FakeStatisticsRepository()
        repository.insertSession(
            session = LockSessionEntity(
                lockId = 1,
                startedAtWallTime = 1_000L,
                completedAtWallTime = 61_000L,
                totalCommittedDuration = 60_000L,
                sourceType = LockSourceType.MOOD,
                sourceId = 1
            ),
            applications = listOf(
                LockSessionApplicationEntity(
                    sessionId = 0,
                    packageId = "com.example.blocked"
                )
            )
        )
        repository.setMostBlockedApplications(
            listOf(
                ApplicationBlockCount(
                    packageId = "com.example.blocked",
                    displayName = "Blocked",
                    blockCount = 1
                )
            )
        )
        repository.setMostFrequentlyUsedMood(
            MoodUsageCount(
                moodId = 1,
                name = "Study",
                useCount = 1
            )
        )

        val viewModel = StatsViewModel(repository)
        advanceUntilIdle()

        val summary = viewModel.uiState.value.summary
        assertEquals("1m", summary.totalDurationLabel)
        assertEquals(1, summary.completedLockSessionCount)
        assertEquals(1, summary.uniqueBlockedApplicationCount)
        assertEquals("Blocked", summary.mostBlockedApplications.single().displayName)
        assertEquals("Study", summary.mostFrequentlyUsedMoodName)
    }
}

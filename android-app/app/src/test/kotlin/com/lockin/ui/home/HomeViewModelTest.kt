package com.lockin.ui.home

import com.lockin.data.entities.LockApplicationEntity
import com.lockin.data.entities.LockEntity
import com.lockin.data.entities.LockSourceType
import com.lockin.data.entities.LockStatus
import com.lockin.testsupport.FakeAppRepository
import com.lockin.testsupport.FakeLockRepository
import com.lockin.testsupport.FakeTimeProvider
import com.lockin.testsupport.MainDispatcherRule
import com.lockin.testsupport.lockableApp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun activeLocksAppearBeforeAnyOtherHomeInformation() = runTest {
        val lockRepository = FakeLockRepository()
        val appRepository = FakeAppRepository(
            listOf(lockableApp("com.example.blocked").copy(displayName = "Blocked"))
        )
        lockRepository.insertLock(
            lock = activeLock(idEnd = 61_000L),
            applications = listOf(lockApp("com.example.blocked"))
        )

        val viewModel = HomeViewModel(
            lockRepository = lockRepository,
            appRepository = appRepository,
            timeProvider = FakeTimeProvider(elapsedTime = 1_000L)
        )
        advanceUntilIdle()

        val activeLock = viewModel.uiState.value.activeLocks.single()
        assertEquals("Lock #1", activeLock.title)
        assertEquals("1m", activeLock.remainingLabel)
        assertEquals(listOf("Blocked"), activeLock.blockedApps)
    }

    private fun activeLock(idEnd: Long): LockEntity =
        LockEntity(
            status = LockStatus.ACTIVE,
            createdAtWallTime = 1_000L,
            startedAtWallTime = 1_000L,
            startedAtElapsedRealtime = 1_000L,
            committedEndWallTime = idEnd,
            remainingDurationAtLastCheckpoint = idEnd - 1_000L,
            lastCheckpointElapsedRealtime = 1_000L,
            sourceType = LockSourceType.MANUAL,
            sourceId = null,
            confirmationTextVersion = 1
        )

    private fun lockApp(packageId: String): LockApplicationEntity =
        LockApplicationEntity(
            lockId = 0,
            packageId = packageId,
            addedAt = 1_000L
        )
}

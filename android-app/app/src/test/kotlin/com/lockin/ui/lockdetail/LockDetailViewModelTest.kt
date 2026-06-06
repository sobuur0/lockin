package com.lockin.ui.lockdetail

import com.lockin.data.entities.LockApplicationEntity
import com.lockin.data.entities.LockEntity
import com.lockin.data.entities.LockSourceType
import com.lockin.data.entities.LockStatus
import com.lockin.domain.lock.LockUseCases
import com.lockin.testsupport.FakeAppRepository
import com.lockin.testsupport.FakeDeviceOwnerState
import com.lockin.testsupport.FakeLockRepository
import com.lockin.testsupport.FakePolicyEnforcer
import com.lockin.testsupport.FakeTimeProvider
import com.lockin.testsupport.MainDispatcherRule
import com.lockin.testsupport.lockableApp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LockDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun refreshRecalculatesRemainingTimeFromCurrentClock() = runTest {
        val timeProvider = FakeTimeProvider(wallTime = 1_000L, elapsedTime = 1_000L)
        val lockRepository = FakeLockRepository()
        val appRepository = FakeAppRepository(
            listOf(lockableApp("com.android.chrome").copy(displayName = "Chrome"))
        )
        val lockId = lockRepository.insertLock(
            lock = activeLock(remainingMillis = 300_000L),
            applications = listOf(lockApp("com.android.chrome"))
        )

        val viewModel = viewModel(
            lockId = lockId,
            lockRepository = lockRepository,
            appRepository = appRepository,
            timeProvider = timeProvider
        )
        advanceUntilIdle()

        assertEquals(300_000L, viewModel.uiState.value.remainingMillis)
        assertEquals("5m 0s", viewModel.uiState.value.remainingLabel)

        timeProvider.wallTime = 2_000L
        timeProvider.elapsedTime = 2_000L
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(299_000L, viewModel.uiState.value.remainingMillis)
        assertEquals("4m 59s", viewModel.uiState.value.remainingLabel)
    }

    private fun viewModel(
        lockId: Long,
        lockRepository: FakeLockRepository,
        appRepository: FakeAppRepository,
        timeProvider: FakeTimeProvider
    ): LockDetailViewModel =
        LockDetailViewModel(
            lockId = lockId,
            lockRepository = lockRepository,
            appRepository = appRepository,
            lockUseCases = LockUseCases(
                appRepository = appRepository,
                lockRepository = lockRepository,
                deviceOwnerState = FakeDeviceOwnerState(),
                timeProvider = timeProvider,
                policyEnforcer = FakePolicyEnforcer()
            ),
            timeProvider = timeProvider
        )

    private fun activeLock(remainingMillis: Long): LockEntity =
        LockEntity(
            status = LockStatus.ACTIVE,
            createdAtWallTime = 1_000L,
            startedAtWallTime = 1_000L,
            startedAtElapsedRealtime = 1_000L,
            committedEndWallTime = 1_000L + remainingMillis,
            remainingDurationAtLastCheckpoint = remainingMillis,
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

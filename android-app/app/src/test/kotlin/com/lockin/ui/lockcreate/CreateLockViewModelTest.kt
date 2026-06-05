package com.lockin.ui.lockcreate

import com.lockin.domain.lock.LockDuration
import com.lockin.domain.lock.LockDurationUnit
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
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class CreateLockViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun rejectsCreateLockWhenNoAppsAreSelected() = runTest {
        val viewModel = viewModel()

        viewModel.setDurationAmount("30")
        viewModel.setConfirmationAccepted(true)
        viewModel.createLock()
        advanceUntilIdle()

        assertEquals(
            "Select at least one app to lock.",
            viewModel.uiState.value.errorMessage
        )
        assertNull(viewModel.uiState.value.createdLockId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun requiresIrreversibleConfirmationBeforeCreatingLock() = runTest {
        val viewModel = viewModel()

        viewModel.toggleApp("com.example.blocked")
        viewModel.setDurationAmount("30")
        viewModel.createLock()
        advanceUntilIdle()

        assertEquals(
            "Confirm the irreversible lock before continuing.",
            viewModel.uiState.value.errorMessage
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createsLockAfterSelectionDurationAndConfirmation() = runTest {
        val viewModel = viewModel()

        viewModel.toggleApp("com.example.blocked")
        viewModel.setDurationAmount("30")
        viewModel.setDurationUnit(LockDurationUnit.MINUTES)
        viewModel.setConfirmationAccepted(true)
        viewModel.createLock()
        advanceUntilIdle()

        assertEquals(1L, viewModel.uiState.value.createdLockId)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun extendsLockOnlyAfterExtensionConfirmation() = runTest {
        val lockRepository = FakeLockRepository()
        val timeProvider = FakeTimeProvider(wallTime = 1_000L, elapsedTime = 1_000L)
        val viewModel = viewModel(
            lockRepository = lockRepository,
            timeProvider = timeProvider
        )

        viewModel.toggleApp("com.example.blocked")
        viewModel.setDurationAmount("30")
        viewModel.setConfirmationAccepted(true)
        viewModel.createLock()
        advanceUntilIdle()

        timeProvider.wallTime = 61_000L
        timeProvider.elapsedTime = 61_000L
        viewModel.setDurationAmount("5")
        viewModel.setDurationUnit(LockDurationUnit.HOURS)
        viewModel.setConfirmationAccepted(false)
        viewModel.extendLock(1L)
        advanceUntilIdle()

        assertEquals(
            "Confirm the irreversible lock before continuing.",
            viewModel.uiState.value.errorMessage
        )

        viewModel.setConfirmationAccepted(true)
        viewModel.extendLock(1L)
        advanceUntilIdle()

        assertEquals(1L, viewModel.uiState.value.extendedLockId)
        assertEquals(
            LockDuration.fromMinutes(29).millis + LockDuration.fromHours(5).millis,
            viewModel.uiState.value.resultingRemainingDuration
        )
    }

    private fun viewModel(
        appRepository: FakeAppRepository = FakeAppRepository(listOf(lockableApp())),
        lockRepository: FakeLockRepository = FakeLockRepository(),
        deviceOwnerState: FakeDeviceOwnerState = FakeDeviceOwnerState(),
        timeProvider: FakeTimeProvider = FakeTimeProvider(),
        policyEnforcer: FakePolicyEnforcer = FakePolicyEnforcer()
    ): CreateLockViewModel =
        CreateLockViewModel(
            appRepository = appRepository,
            lockUseCases = LockUseCases(
                appRepository = appRepository,
                lockRepository = lockRepository,
                deviceOwnerState = deviceOwnerState,
                timeProvider = timeProvider,
                policyEnforcer = policyEnforcer
            )
        )
}

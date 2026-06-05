package com.lockin.contracts

import com.lockin.data.entities.PolicyReconciliationTrigger
import com.lockin.domain.lock.CreateLockRequest
import com.lockin.domain.lock.LockDuration
import com.lockin.domain.lock.LockUseCaseResult
import com.lockin.domain.lock.LockUseCases
import com.lockin.testsupport.FakeAppRepository
import com.lockin.testsupport.FakeDeviceOwnerState
import com.lockin.testsupport.FakeLockRepository
import com.lockin.testsupport.FakePolicyEnforcer
import com.lockin.testsupport.FakeTimeProvider
import com.lockin.testsupport.lockableApp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateLockContractTest {
    @Test
    fun rejectsLockCreationWithoutDeviceOwnerStatus() = runTest {
        val ownerState = FakeDeviceOwnerState(isDeviceOwner = false)
        val useCases = useCases(deviceOwnerState = ownerState)

        val result = useCases.createLock(
            CreateLockRequest(
                packageIds = setOf("com.example.blocked"),
                duration = LockDuration.fromMinutes(30)
            )
        )

        assertTrue(result is LockUseCaseResult.Rejected)
        assertEquals(
            "Device Owner status is required to create locks.",
            (result as LockUseCaseResult.Rejected).reason
        )
    }

    @Test
    fun rejectsCreateLockWithEmptyAppSelection() = runTest {
        val result = useCases().createLock(
            CreateLockRequest(
                packageIds = emptySet(),
                duration = LockDuration.fromMinutes(30)
            )
        )

        assertTrue(result is LockUseCaseResult.Rejected)
        assertEquals(
            "Select at least one app to lock.",
            (result as LockUseCaseResult.Rejected).reason
        )
    }

    @Test
    fun rejectsPolicyExemptAppBeforeCreatingLock() = runTest {
        val appRepository = FakeAppRepository(
            listOf(lockableApp().copy(isPolicyExempt = true))
        )
        val result = useCases(appRepository = appRepository).createLock(
            CreateLockRequest(
                packageIds = setOf("com.example.blocked"),
                duration = LockDuration.fromMinutes(30)
            )
        )

        assertTrue(result is LockUseCaseResult.Rejected)
        assertEquals(
            "App is required by device policy.",
            (result as LockUseCaseResult.Rejected).reason
        )
    }

    @Test
    fun persistsLockAndRequestsPolicyReconciliationOnSuccess() = runTest {
        val policyEnforcer = FakePolicyEnforcer()
        val result = useCases(policyEnforcer = policyEnforcer).createLock(
            CreateLockRequest(
                packageIds = setOf("com.example.blocked"),
                duration = LockDuration.fromMinutes(30)
            )
        )

        assertEquals(LockUseCaseResult.Created(lockId = 1L), result)
        assertEquals(
            listOf(PolicyReconciliationTrigger.LOCK_CREATED),
            policyEnforcer.triggers
        )
    }

    private fun useCases(
        appRepository: FakeAppRepository = FakeAppRepository(listOf(lockableApp())),
        lockRepository: FakeLockRepository = FakeLockRepository(),
        deviceOwnerState: FakeDeviceOwnerState = FakeDeviceOwnerState(),
        timeProvider: FakeTimeProvider = FakeTimeProvider(),
        policyEnforcer: FakePolicyEnforcer = FakePolicyEnforcer()
    ): LockUseCases =
        LockUseCases(
            appRepository = appRepository,
            lockRepository = lockRepository,
            deviceOwnerState = deviceOwnerState,
            timeProvider = timeProvider,
            policyEnforcer = policyEnforcer
        )
}

package com.lockin.domain.lock

import com.lockin.data.entities.PolicyReconciliationTrigger
import com.lockin.testsupport.FakeAppRepository
import com.lockin.testsupport.FakeDeviceOwnerState
import com.lockin.testsupport.FakeLockRepository
import com.lockin.testsupport.FakePolicyEnforcer
import com.lockin.testsupport.FakeTimeProvider
import com.lockin.testsupport.lockableApp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LockExpirationSchedulerTest {
    @Test
    fun creatingLockSchedulesExpirationWhileAppMayBeIdle() = runTest {
        val scheduler = FakeLockExpirationScheduler()

        LockUseCases(
            appRepository = FakeAppRepository(listOf(lockableApp())),
            lockRepository = FakeLockRepository(),
            deviceOwnerState = FakeDeviceOwnerState(),
            timeProvider = FakeTimeProvider(),
            policyEnforcer = FakePolicyEnforcer(),
            lockExpirationScheduler = scheduler
        ).createLock(
            CreateLockRequest(
                packageIds = setOf("com.example.blocked"),
                duration = LockDuration.fromMinutes(10)
            )
        )

        assertEquals(1, scheduler.scheduleCount)
    }

    @Test
    fun scheduledExpirationReleasesPackageAfterDurationElapsed() = runTest {
        val lockRepository = FakeLockRepository()
        val policyEnforcer = FakePolicyEnforcer()
        val timeProvider = FakeTimeProvider(wallTime = 1_000L, elapsedTime = 1_000L)
        LockUseCases(
            appRepository = FakeAppRepository(listOf(lockableApp())),
            lockRepository = lockRepository,
            deviceOwnerState = FakeDeviceOwnerState(),
            timeProvider = timeProvider,
            policyEnforcer = policyEnforcer,
            lockExpirationScheduler = FakeLockExpirationScheduler()
        ).createLock(
            CreateLockRequest(
                packageIds = setOf("com.example.blocked"),
                duration = LockDuration.fromMinutes(10)
            )
        )

        timeProvider.wallTime = 601_000L
        timeProvider.elapsedTime = 601_000L

        LockExpirationReconciler(
            lockRepository = lockRepository,
            policyEnforcer = policyEnforcer,
            timeProvider = timeProvider
        ).reconcile(PolicyReconciliationTrigger.LOCK_EXPIRED)

        assertEquals(listOf(setOf("com.example.blocked")), policyEnforcer.releasedPackageIds)
    }

}

private class FakeLockExpirationScheduler : LockExpirationScheduler {
    var scheduleCount = 0
    var cancelCount = 0

    override suspend fun scheduleNextExpiration() {
        scheduleCount += 1
    }

    override fun cancel() {
        cancelCount += 1
    }
}

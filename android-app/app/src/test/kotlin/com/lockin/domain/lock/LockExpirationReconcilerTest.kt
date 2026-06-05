package com.lockin.domain.lock

import com.lockin.data.entities.LockApplicationEntity
import com.lockin.data.entities.LockEntity
import com.lockin.data.entities.LockSourceType
import com.lockin.data.entities.LockStatus
import com.lockin.data.entities.PolicyReconciliationTrigger
import com.lockin.testsupport.FakeLockRepository
import com.lockin.testsupport.FakePolicyEnforcer
import com.lockin.testsupport.FakeTimeProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LockExpirationReconcilerTest {
    @Test
    fun completesExpiredLockAndReleasesPackages() = runTest {
        val lockRepository = FakeLockRepository()
        val policyEnforcer = FakePolicyEnforcer()
        val timeProvider = FakeTimeProvider(wallTime = 11_000L, elapsedTime = 11_000L)
        val lockId = lockRepository.insertLock(
            lock = testLock(endWallTime = 10_000L, remainingMillis = 9_000L),
            applications = listOf(lockApp("com.example.blocked"))
        )

        val summary = LockExpirationReconciler(
            lockRepository = lockRepository,
            policyEnforcer = policyEnforcer,
            timeProvider = timeProvider
        ).reconcile(PolicyReconciliationTrigger.APP_START)

        assertEquals(setOf(lockId), summary.completedLockIds)
        assertEquals(setOf("com.example.blocked"), summary.releasedPackageIds)
        assertEquals(LockStatus.COMPLETED, lockRepository.getLock(lockId)?.status)
        assertEquals(listOf(setOf("com.example.blocked")), policyEnforcer.releasedPackageIds)
    }

    @Test
    fun doesNotReleasePackageCoveredByAnotherActiveLock() = runTest {
        val lockRepository = FakeLockRepository()
        val policyEnforcer = FakePolicyEnforcer()
        val timeProvider = FakeTimeProvider(wallTime = 11_000L, elapsedTime = 11_000L)
        val expiredLockId = lockRepository.insertLock(
            lock = testLock(endWallTime = 10_000L, remainingMillis = 9_000L),
            applications = listOf(lockApp("com.example.blocked"))
        )
        val activeLockId = lockRepository.insertLock(
            lock = testLock(endWallTime = 20_000L, remainingMillis = 20_000L),
            applications = listOf(lockApp("com.example.blocked"))
        )

        val summary = LockExpirationReconciler(
            lockRepository = lockRepository,
            policyEnforcer = policyEnforcer,
            timeProvider = timeProvider
        ).reconcile(PolicyReconciliationTrigger.APP_START)

        assertEquals(setOf(expiredLockId), summary.completedLockIds)
        assertEquals(emptySet<String>(), summary.releasedPackageIds)
        assertEquals(setOf("com.example.blocked"), summary.remainingActivePackageIds)
        assertEquals(LockStatus.COMPLETED, lockRepository.getLock(expiredLockId)?.status)
        assertEquals(LockStatus.ACTIVE, lockRepository.getLock(activeLockId)?.status)
        assertEquals(listOf(emptySet<String>()), policyEnforcer.releasedPackageIds)
    }

    private fun testLock(
        endWallTime: Long,
        remainingMillis: Long
    ): LockEntity =
        LockEntity(
            status = LockStatus.ACTIVE,
            createdAtWallTime = 1_000L,
            startedAtWallTime = 1_000L,
            startedAtElapsedRealtime = 1_000L,
            committedEndWallTime = endWallTime,
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

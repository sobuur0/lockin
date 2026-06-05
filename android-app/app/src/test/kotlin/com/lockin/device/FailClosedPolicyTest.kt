package com.lockin.device

import android.content.ComponentName
import com.lockin.data.entities.LockApplicationEntity
import com.lockin.data.entities.LockEntity
import com.lockin.data.entities.LockSourceType
import com.lockin.data.entities.LockStatus
import com.lockin.data.entities.PolicyReconciliationResult
import com.lockin.data.entities.PolicyReconciliationTrigger
import com.lockin.testsupport.FakeLockRepository
import com.lockin.testsupport.FakePolicyEventRepository
import com.lockin.testsupport.FakeTimeProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FailClosedPolicyTest {
    @Test
    fun marksLockFailedClosedWhenPolicyApplicationFails() = runTest {
        val lockRepository = FakeLockRepository()
        val policyEvents = FakePolicyEventRepository()
        val gateway = FakeDevicePolicyGateway(
            hiddenResult = DevicePolicyResult.Failed(IllegalStateException("hidden failed"))
        )
        val lockId = lockRepository.insertLock(
            lock = testLock(),
            applications = listOf(lockApp("com.example.blocked"))
        )

        val summary = LockPolicyEnforcer(
            lockRepository = lockRepository,
            policyEventRepository = policyEvents,
            devicePolicyGateway = gateway,
            timeProvider = FakeTimeProvider()
        ).enforceActiveLocks(PolicyReconciliationTrigger.APP_START)

        assertEquals(setOf("com.example.blocked"), summary.failedPackageIds)
        assertEquals(LockStatus.FAILED_CLOSED, lockRepository.getLock(lockId)?.status)
        assertEquals(PolicyReconciliationResult.FAILED_CLOSED, policyEvents.events.last().result)
    }

    @Test
    fun restoresFailedClosedLockToActiveWhenPolicyApplicationSucceeds() = runTest {
        val lockRepository = FakeLockRepository()
        val lockId = lockRepository.insertLock(
            lock = testLock(status = LockStatus.FAILED_CLOSED),
            applications = listOf(lockApp("com.example.blocked"))
        )

        LockPolicyEnforcer(
            lockRepository = lockRepository,
            policyEventRepository = FakePolicyEventRepository(),
            devicePolicyGateway = FakeDevicePolicyGateway(),
            timeProvider = FakeTimeProvider()
        ).enforceActiveLocks(PolicyReconciliationTrigger.APP_START)

        assertEquals(LockStatus.ACTIVE, lockRepository.getLock(lockId)?.status)
    }

    @Test
    fun marksLocksFailedClosedWhenDeviceOwnerIsMissing() = runTest {
        val lockRepository = FakeLockRepository()
        val lockId = lockRepository.insertLock(
            lock = testLock(),
            applications = listOf(lockApp("com.example.blocked"))
        )

        LockPolicyEnforcer(
            lockRepository = lockRepository,
            policyEventRepository = FakePolicyEventRepository(),
            devicePolicyGateway = FakeDevicePolicyGateway(deviceOwner = false),
            timeProvider = FakeTimeProvider()
        ).enforceActiveLocks(PolicyReconciliationTrigger.APP_START)

        assertEquals(LockStatus.FAILED_CLOSED, lockRepository.getLock(lockId)?.status)
    }

    private fun testLock(status: LockStatus = LockStatus.ACTIVE): LockEntity =
        LockEntity(
            status = status,
            createdAtWallTime = 1_000L,
            startedAtWallTime = 1_000L,
            startedAtElapsedRealtime = 1_000L,
            committedEndWallTime = 10_000L,
            remainingDurationAtLastCheckpoint = 9_000L,
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

private class FakeDevicePolicyGateway(
    private val deviceOwner: Boolean = true,
    private val hiddenResult: DevicePolicyResult = DevicePolicyResult.Applied,
    private val suspendedResult: PackageSuspensionResult = PackageSuspensionResult(DevicePolicyResult.Applied),
    private val uninstallResult: DevicePolicyResult = DevicePolicyResult.Applied
) : DevicePolicyGateway {
    override val adminComponent: ComponentName = ComponentName("com.lockin", "Receiver")

    override fun isDeviceOwner(): Boolean = deviceOwner

    override fun setApplicationHidden(packageId: String, hidden: Boolean): DevicePolicyResult =
        hiddenResult

    override fun isApplicationHidden(packageId: String): Boolean = false

    override fun setPackagesSuspended(
        packageIds: Set<String>,
        suspended: Boolean
    ): PackageSuspensionResult = suspendedResult

    override fun setUninstallBlocked(packageId: String, blocked: Boolean): DevicePolicyResult =
        uninstallResult

    override fun isUninstallBlocked(packageId: String): Boolean = false

    override fun addUserRestriction(restriction: String): DevicePolicyResult =
        DevicePolicyResult.Applied

    override fun clearUserRestriction(restriction: String): DevicePolicyResult =
        DevicePolicyResult.Applied
}

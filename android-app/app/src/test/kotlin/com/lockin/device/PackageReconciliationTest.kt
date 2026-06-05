package com.lockin.device

import com.lockin.data.entities.LockApplicationEntity
import com.lockin.data.entities.LockEntity
import com.lockin.data.entities.LockSourceType
import com.lockin.data.entities.LockStatus
import com.lockin.data.entities.PolicyReconciliationTrigger
import com.lockin.domain.appcatalog.AppCatalogScanner
import com.lockin.domain.appcatalog.AppIdentity
import com.lockin.domain.lock.LockExpirationReconciler
import com.lockin.domain.lock.LockExpirationScheduler
import com.lockin.testsupport.FakeAppRepository
import com.lockin.testsupport.FakeLockRepository
import com.lockin.testsupport.FakePolicyEnforcer
import com.lockin.testsupport.FakeTimeProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PackageReconciliationTest {
    @Test
    fun packageChangeRefreshesInstalledIdentityAndReappliesActivePolicyByPackageId() = runTest {
        val appRepository = FakeAppRepository()
        val lockRepository = FakeLockRepository()
        val policyEnforcer = FakePolicyEnforcer()
        val scheduler = FakeScheduler()
        lockRepository.insertLock(
            lock = testLock(),
            applications = listOf(lockApp("com.example.blocked"))
        )

        LockReconciliationRunner(
            appCatalogScanner = FakeScanner(
                listOf(
                    AppIdentity(
                        packageId = "com.example.blocked",
                        displayName = "Blocked",
                        iconRef = null,
                        isInstalled = true,
                        isLockinApp = false,
                        isPolicyExempt = false
                    )
                )
            ),
            appRepository = appRepository,
            lockExpirationReconciler = LockExpirationReconciler(
                lockRepository = lockRepository,
                policyEnforcer = policyEnforcer,
                timeProvider = FakeTimeProvider()
            ),
            lockExpirationScheduler = scheduler
        ).reconcile(
            trigger = PolicyReconciliationTrigger.PACKAGE_CHANGED,
            refreshInstalledApps = true
        )

        assertNotNull(appRepository.getApp("com.example.blocked"))
        assertEquals(listOf(PolicyReconciliationTrigger.PACKAGE_CHANGED), policyEnforcer.triggers)
        assertEquals(1, scheduler.scheduleCount)
    }

    private fun testLock(): LockEntity =
        LockEntity(
            status = LockStatus.ACTIVE,
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

private class FakeScanner(
    private val apps: List<AppIdentity>
) : AppCatalogScanner {
    override suspend fun scanInstalledApps(): List<AppIdentity> = apps
}

private class FakeScheduler : LockExpirationScheduler {
    var scheduleCount = 0
    var cancelCount = 0

    override suspend fun scheduleNextExpiration() {
        scheduleCount += 1
    }

    override fun cancel() {
        cancelCount += 1
    }
}

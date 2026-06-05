package com.lockin.device

import com.lockin.data.entities.ApplicationEntity
import com.lockin.data.entities.PolicyReconciliationTrigger
import com.lockin.domain.appcatalog.AppCatalogScanner
import com.lockin.domain.lock.LockExpirationReconciler
import com.lockin.domain.lock.LockExpirationScheduler
import com.lockin.domain.repository.AppRepository

class LockReconciliationRunner(
    private val appCatalogScanner: AppCatalogScanner,
    private val appRepository: AppRepository,
    private val lockExpirationReconciler: LockExpirationReconciler,
    private val lockExpirationScheduler: LockExpirationScheduler
) {
    suspend fun reconcile(
        trigger: PolicyReconciliationTrigger,
        refreshInstalledApps: Boolean
    ) {
        if (refreshInstalledApps) {
            refreshInstalledApps()
        }
        lockExpirationReconciler.reconcile(trigger)
        lockExpirationScheduler.scheduleNextExpiration()
    }

    private suspend fun refreshInstalledApps() {
        val now = System.currentTimeMillis()
        val applications = appCatalogScanner.scanInstalledApps().map { identity ->
            ApplicationEntity(
                packageId = identity.packageId,
                displayName = identity.displayName,
                iconRef = identity.iconRef,
                isInstalled = identity.isInstalled,
                isLockinApp = identity.isLockinApp,
                isPolicyExempt = identity.isPolicyExempt,
                lastSeenAt = now
            )
        }
        appRepository.upsertApps(applications)
    }
}

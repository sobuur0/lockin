package com.lockin.domain.lock

import com.lockin.data.entities.LockApplicationEntity
import com.lockin.data.entities.LockEntity
import com.lockin.data.entities.LockSourceType
import com.lockin.data.entities.LockStatus
import com.lockin.data.entities.PolicyReconciliationTrigger
import com.lockin.device.ActiveLockPolicyEnforcer
import com.lockin.device.DeviceOwnerState
import com.lockin.domain.appcatalog.AppEligibilityRules
import com.lockin.domain.repository.AppRepository
import com.lockin.domain.repository.LockRepository

sealed interface LockUseCaseResult {
    data class Created(val lockId: Long) : LockUseCaseResult
    data class Extended(val lockId: Long, val resultingRemainingDuration: Long) : LockUseCaseResult
    data class Rejected(val reason: String) : LockUseCaseResult
}

data class CreateLockRequest(
    val packageIds: Set<String>,
    val duration: LockDuration,
    val sourceType: LockSourceType = LockSourceType.MANUAL,
    val sourceId: Long? = null,
    val confirmationTextVersion: Int = CURRENT_CONFIRMATION_TEXT_VERSION
) {
    companion object {
        const val CURRENT_CONFIRMATION_TEXT_VERSION = 1
    }
}

class LockUseCases(
    private val appRepository: AppRepository,
    private val lockRepository: LockRepository,
    private val deviceOwnerState: DeviceOwnerState,
    private val timeProvider: TimeProvider,
    private val policyEnforcer: ActiveLockPolicyEnforcer
) {
    suspend fun createLock(request: CreateLockRequest): LockUseCaseResult {
        if (!deviceOwnerState.currentStatus().isDeviceOwner) {
            return LockUseCaseResult.Rejected("Device Owner status is required to create locks.")
        }
        if (request.packageIds.isEmpty()) {
            return LockUseCaseResult.Rejected("Select at least one app to lock.")
        }

        val apps = request.packageIds.map { packageId ->
            appRepository.getApp(packageId)
                ?: return LockUseCaseResult.Rejected("App is unavailable: $packageId")
        }
        apps.forEach { app ->
            runCatching { AppEligibilityRules.requireLockable(app) }
                .onFailure { return LockUseCaseResult.Rejected(it.message ?: "App cannot be locked.") }
        }

        val nowWallTime = timeProvider.wallTimeMillis()
        val nowElapsed = timeProvider.elapsedRealtimeMillis()
        val lock = LockEntity(
            status = LockStatus.ACTIVE,
            createdAtWallTime = nowWallTime,
            startedAtWallTime = nowWallTime,
            startedAtElapsedRealtime = nowElapsed,
            committedEndWallTime = LockTiming.committedEndWallTime(nowWallTime, request.duration),
            remainingDurationAtLastCheckpoint = request.duration.millis,
            lastCheckpointElapsedRealtime = nowElapsed,
            sourceType = request.sourceType,
            sourceId = request.sourceId,
            confirmationTextVersion = request.confirmationTextVersion
        )
        val lockApplications = request.packageIds.map { packageId ->
            LockApplicationEntity(
                lockId = 0,
                packageId = packageId,
                addedAt = nowWallTime
            )
        }
        val lockId = lockRepository.insertLock(lock, lockApplications)
        policyEnforcer.enforceActiveLocks(PolicyReconciliationTrigger.LOCK_CREATED)
        return LockUseCaseResult.Created(lockId)
    }

    suspend fun extendLock(lockId: Long, extension: LockDuration): LockUseCaseResult {
        if (!deviceOwnerState.currentStatus().isDeviceOwner) {
            return LockUseCaseResult.Rejected("Device Owner status is required to extend locks.")
        }
        val lock = lockRepository.getLock(lockId)
            ?: return LockUseCaseResult.Rejected("Lock does not exist.")
        if (lock.status == LockStatus.COMPLETED) {
            return LockUseCaseResult.Rejected("Completed locks cannot be extended.")
        }

        val nowWallTime = timeProvider.wallTimeMillis()
        val nowElapsed = timeProvider.elapsedRealtimeMillis()
        val remaining = LockTiming.remainingDuration(lock, nowElapsed)
        val calculation = LockTiming.calculateExtension(
            currentRemainingMillis = remaining.millis,
            extension = extension,
            nowWallTime = nowWallTime
        )
        val updatedLock = lock.copy(
            committedEndWallTime = calculation.resultingEndWallTime,
            remainingDurationAtLastCheckpoint = calculation.resultingRemainingDuration,
            lastCheckpointElapsedRealtime = nowElapsed
        )

        lockRepository.updateLock(updatedLock)
        lockRepository.addExtension(
            LockTiming.extensionEntity(
                lockId = lockId,
                calculation = calculation,
                confirmedAtWallTime = nowWallTime,
                confirmedAtElapsedRealtime = nowElapsed
            )
        )
        policyEnforcer.enforceActiveLocks(PolicyReconciliationTrigger.LOCK_EXTENDED)
        return LockUseCaseResult.Extended(
            lockId = lockId,
            resultingRemainingDuration = calculation.resultingRemainingDuration
        )
    }
}

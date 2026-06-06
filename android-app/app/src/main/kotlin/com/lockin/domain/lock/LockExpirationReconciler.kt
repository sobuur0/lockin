package com.lockin.domain.lock

import com.lockin.data.entities.LockStatus
import com.lockin.data.entities.PolicyReconciliationTrigger
import com.lockin.device.ActiveLockPolicyEnforcer
import com.lockin.domain.repository.LockRepository
import com.lockin.domain.statistics.LockSessionRecorder
import kotlinx.coroutines.flow.first

data class LockExpirationReconciliationSummary(
    val completedLockIds: Set<Long>,
    val releasedPackageIds: Set<String>,
    val remainingActivePackageIds: Set<String>
)

class LockExpirationReconciler(
    private val lockRepository: LockRepository,
    private val policyEnforcer: ActiveLockPolicyEnforcer,
    private val timeProvider: TimeProvider,
    private val lockSessionRecorder: LockSessionRecorder? = null
) {
    suspend fun reconcile(
        trigger: PolicyReconciliationTrigger
    ): LockExpirationReconciliationSummary {
        val activeLocks = lockRepository.observeActiveLocks().first()
        val nowElapsed = timeProvider.elapsedRealtimeMillis()
        val nowWall = timeProvider.wallTimeMillis()
        val expiredLocks = activeLocks.filter { lockWithApplications ->
            val lock = lockWithApplications.lock
            LockTiming.remainingDuration(
                lock = lock,
                nowElapsedRealtime = nowElapsed,
                nowWallTime = nowWall
            ).isElapsed ||
                nowWall >= lock.committedEndWallTime
        }

        val expiredPackageIds = expiredLocks
            .flatMap { it.applications }
            .map { it.packageId }
            .toSet()

        expiredLocks.forEach { lockWithApplications ->
            lockSessionRecorder?.recordCompletedLock(
                lockWithApplications = lockWithApplications,
                completedAtWallTime = nowWall
            )
            lockRepository.updateLock(
                lockWithApplications.lock.copy(
                    status = LockStatus.COMPLETED,
                    remainingDurationAtLastCheckpoint = 0,
                    lastCheckpointElapsedRealtime = nowElapsed
                )
            )
        }

        val remainingActivePackageIds = lockRepository.observeActivePackageIds().first().toSet()
        val releasablePackageIds = expiredPackageIds - remainingActivePackageIds

        policyEnforcer.releasePackages(
            packageIds = releasablePackageIds,
            trigger = trigger
        )
        policyEnforcer.enforceActiveLocks(trigger)

        return LockExpirationReconciliationSummary(
            completedLockIds = expiredLocks.map { it.lock.id }.toSet(),
            releasedPackageIds = releasablePackageIds,
            remainingActivePackageIds = remainingActivePackageIds
        )
    }
}

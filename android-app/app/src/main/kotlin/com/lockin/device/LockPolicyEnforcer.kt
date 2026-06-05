package com.lockin.device

import com.lockin.data.entities.PolicyReconciliationEventEntity
import com.lockin.data.entities.PolicyReconciliationResult
import com.lockin.data.entities.PolicyReconciliationTrigger
import com.lockin.domain.lock.TimeProvider
import com.lockin.domain.repository.LockRepository
import com.lockin.domain.repository.PolicyEventRepository
import kotlinx.coroutines.flow.first

data class LockPolicyEnforcementSummary(
    val trigger: PolicyReconciliationTrigger,
    val packageCount: Int,
    val blockedPackageIds: Set<String>,
    val failedPackageIds: Set<String>,
    val deviceOwnerMissing: Boolean
)

interface ActiveLockPolicyEnforcer {
    suspend fun enforceActiveLocks(
        trigger: PolicyReconciliationTrigger
    ): LockPolicyEnforcementSummary

    suspend fun releasePackages(
        packageIds: Set<String>,
        trigger: PolicyReconciliationTrigger
    ): LockPolicyEnforcementSummary
}

class LockPolicyEnforcer(
    private val lockRepository: LockRepository,
    private val policyEventRepository: PolicyEventRepository,
    private val devicePolicyGateway: DevicePolicyGateway,
    private val timeProvider: TimeProvider
) : ActiveLockPolicyEnforcer {
    override suspend fun enforceActiveLocks(
        trigger: PolicyReconciliationTrigger
    ): LockPolicyEnforcementSummary {
        val activePackageIds = lockRepository.observeActivePackageIds().first().toSet()

        if (!devicePolicyGateway.isDeviceOwner()) {
            record(
                trigger = trigger,
                packageId = null,
                result = PolicyReconciliationResult.DEVICE_OWNER_MISSING,
                message = "Device Owner status is required before lock policies can be applied."
            )
            return LockPolicyEnforcementSummary(
                trigger = trigger,
                packageCount = activePackageIds.size,
                blockedPackageIds = emptySet(),
                failedPackageIds = activePackageIds,
                deviceOwnerMissing = true
            )
        }

        val failedPackageIds = mutableSetOf<String>()
        activePackageIds.forEach { packageId ->
            if (devicePolicyGateway.setApplicationHidden(packageId, hidden = true).isFailure()) {
                failedPackageIds += packageId
            }
            if (devicePolicyGateway.setUninstallBlocked(packageId, blocked = true).isFailure()) {
                failedPackageIds += packageId
            }
        }

        val suspension = if (activePackageIds.isEmpty()) {
            PackageSuspensionResult(DevicePolicyResult.AlreadyApplied)
        } else {
            devicePolicyGateway.setPackagesSuspended(activePackageIds, suspended = true)
        }
        failedPackageIds += suspension.failedPackageIds
        if (suspension.result.isFailure()) {
            failedPackageIds += activePackageIds
        }

        activePackageIds.forEach { packageId ->
            val failed = packageId in failedPackageIds
            record(
                trigger = trigger,
                packageId = packageId,
                result = if (failed) {
                    PolicyReconciliationResult.FAILED_CLOSED
                } else {
                    PolicyReconciliationResult.APPLIED
                },
                message = if (failed) {
                    "Policy application was uncertain; package remains treated as locked."
                } else {
                    "Active lock policy applied."
                }
            )
        }

        return LockPolicyEnforcementSummary(
            trigger = trigger,
            packageCount = activePackageIds.size,
            blockedPackageIds = activePackageIds - failedPackageIds,
            failedPackageIds = failedPackageIds,
            deviceOwnerMissing = false
        )
    }

    override suspend fun releasePackages(
        packageIds: Set<String>,
        trigger: PolicyReconciliationTrigger
    ): LockPolicyEnforcementSummary {
        if (packageIds.isEmpty()) {
            return LockPolicyEnforcementSummary(
                trigger = trigger,
                packageCount = 0,
                blockedPackageIds = emptySet(),
                failedPackageIds = emptySet(),
                deviceOwnerMissing = false
            )
        }

        if (!devicePolicyGateway.isDeviceOwner()) {
            record(
                trigger = trigger,
                packageId = null,
                result = PolicyReconciliationResult.DEVICE_OWNER_MISSING,
                message = "Device Owner status is required before lock policies can be released."
            )
            return LockPolicyEnforcementSummary(
                trigger = trigger,
                packageCount = packageIds.size,
                blockedPackageIds = emptySet(),
                failedPackageIds = packageIds,
                deviceOwnerMissing = true
            )
        }

        val failedPackageIds = mutableSetOf<String>()
        val suspension = devicePolicyGateway.setPackagesSuspended(packageIds, suspended = false)
        failedPackageIds += suspension.failedPackageIds
        if (suspension.result.isFailure()) {
            failedPackageIds += packageIds
        }

        packageIds.forEach { packageId ->
            if (devicePolicyGateway.setApplicationHidden(packageId, hidden = false).isFailure()) {
                failedPackageIds += packageId
            }
            if (devicePolicyGateway.setUninstallBlocked(packageId, blocked = false).isFailure()) {
                failedPackageIds += packageId
            }
        }

        packageIds.forEach { packageId ->
            val failed = packageId in failedPackageIds
            record(
                trigger = trigger,
                packageId = packageId,
                result = if (failed) {
                    PolicyReconciliationResult.FAILED_CLOSED
                } else {
                    PolicyReconciliationResult.APPLIED
                },
                message = if (failed) {
                    "Expired lock policy release failed; package may remain blocked."
                } else {
                    "Expired lock policy released."
                }
            )
        }

        return LockPolicyEnforcementSummary(
            trigger = trigger,
            packageCount = packageIds.size,
            blockedPackageIds = emptySet(),
            failedPackageIds = failedPackageIds,
            deviceOwnerMissing = false
        )
    }

    private suspend fun record(
        trigger: PolicyReconciliationTrigger,
        packageId: String?,
        result: PolicyReconciliationResult,
        message: String
    ) {
        policyEventRepository.record(
            PolicyReconciliationEventEntity(
                occurredAt = timeProvider.wallTimeMillis(),
                trigger = trigger,
                packageId = packageId,
                result = result,
                message = message
            )
        )
    }

    private fun DevicePolicyResult.isFailure(): Boolean =
        this is DevicePolicyResult.Failed || this is DevicePolicyResult.DeviceOwnerMissing
}

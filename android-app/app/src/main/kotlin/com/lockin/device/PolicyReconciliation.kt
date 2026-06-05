package com.lockin.device

import android.content.Intent
import com.lockin.data.entities.PolicyReconciliationResult
import com.lockin.data.entities.PolicyReconciliationTrigger

object PolicyReconciliation {
    const val ACTION_EXPIRE_LOCKS = "com.lockin.action.EXPIRE_LOCKS"

    fun triggerForPackageIntent(intent: Intent): PolicyReconciliationTrigger? =
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_CHANGED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_REPLACED -> PolicyReconciliationTrigger.PACKAGE_CHANGED
            else -> null
        }

    fun triggerForBootIntent(intent: Intent): PolicyReconciliationTrigger? =
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> PolicyReconciliationTrigger.BOOT
            else -> null
        }

    fun DevicePolicyResult.toPolicyResult(): PolicyReconciliationResult =
        when (this) {
            DevicePolicyResult.Applied -> PolicyReconciliationResult.APPLIED
            DevicePolicyResult.AlreadyApplied -> PolicyReconciliationResult.ALREADY_APPLIED
            DevicePolicyResult.DeviceOwnerMissing -> PolicyReconciliationResult.DEVICE_OWNER_MISSING
            is DevicePolicyResult.Failed -> PolicyReconciliationResult.FAILED_CLOSED
        }
}

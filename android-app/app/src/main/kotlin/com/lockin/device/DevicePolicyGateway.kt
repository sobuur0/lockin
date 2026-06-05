package com.lockin.device

import android.content.ComponentName

sealed interface DevicePolicyResult {
    data object Applied : DevicePolicyResult
    data object AlreadyApplied : DevicePolicyResult
    data object DeviceOwnerMissing : DevicePolicyResult
    data class Failed(val throwable: Throwable) : DevicePolicyResult
}

data class PackageSuspensionResult(
    val result: DevicePolicyResult,
    val failedPackageIds: Set<String> = emptySet()
)

interface DevicePolicyGateway {
    val adminComponent: ComponentName

    fun isDeviceOwner(): Boolean

    fun setApplicationHidden(packageId: String, hidden: Boolean): DevicePolicyResult

    fun isApplicationHidden(packageId: String): Boolean

    fun setPackagesSuspended(packageIds: Set<String>, suspended: Boolean): PackageSuspensionResult

    fun setUninstallBlocked(packageId: String, blocked: Boolean): DevicePolicyResult

    fun isUninstallBlocked(packageId: String): Boolean

    fun addUserRestriction(restriction: String): DevicePolicyResult

    fun clearUserRestriction(restriction: String): DevicePolicyResult
}

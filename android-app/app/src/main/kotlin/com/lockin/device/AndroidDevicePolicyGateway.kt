package com.lockin.device

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context

class AndroidDeviceOwnerState(
    private val context: Context,
    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(DevicePolicyManager::class.java)
) : DeviceOwnerState {
    override fun currentStatus(): DeviceOwnerStatus =
        DeviceOwnerStatus(
            isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(context.packageName),
            packageName = context.packageName
        )
}

class AndroidDevicePolicyGateway(
    private val context: Context,
    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(DevicePolicyManager::class.java)
) : DevicePolicyGateway {
    override val adminComponent: ComponentName =
        ComponentName(context.packageName, "$RECEIVER_PACKAGE.LockinDeviceAdminReceiver")

    override fun isDeviceOwner(): Boolean =
        devicePolicyManager.isDeviceOwnerApp(context.packageName)

    override fun setApplicationHidden(packageId: String, hidden: Boolean): DevicePolicyResult =
        runIfDeviceOwner {
            val changed = devicePolicyManager.setApplicationHidden(adminComponent, packageId, hidden)
            if (changed) DevicePolicyResult.Applied else DevicePolicyResult.AlreadyApplied
        }

    override fun isApplicationHidden(packageId: String): Boolean =
        devicePolicyManager.isApplicationHidden(adminComponent, packageId)

    override fun setPackagesSuspended(
        packageIds: Set<String>,
        suspended: Boolean
    ): PackageSuspensionResult {
        if (!isDeviceOwner()) {
            return PackageSuspensionResult(DevicePolicyResult.DeviceOwnerMissing)
        }

        return try {
            val failed = devicePolicyManager
                .setPackagesSuspended(adminComponent, packageIds.toTypedArray(), suspended)
                .toSet()
            val result = if (failed.isEmpty()) {
                DevicePolicyResult.Applied
            } else {
                DevicePolicyResult.Failed(
                    IllegalStateException("Some packages could not be suspended.")
                )
            }
            PackageSuspensionResult(result = result, failedPackageIds = failed)
        } catch (throwable: Throwable) {
            PackageSuspensionResult(DevicePolicyResult.Failed(throwable))
        }
    }

    override fun setUninstallBlocked(packageId: String, blocked: Boolean): DevicePolicyResult =
        runIfDeviceOwner {
            devicePolicyManager.setUninstallBlocked(adminComponent, packageId, blocked)
            DevicePolicyResult.Applied
        }

    override fun isUninstallBlocked(packageId: String): Boolean =
        devicePolicyManager.isUninstallBlocked(adminComponent, packageId)

    override fun addUserRestriction(restriction: String): DevicePolicyResult =
        runIfDeviceOwner {
            devicePolicyManager.addUserRestriction(adminComponent, restriction)
            DevicePolicyResult.Applied
        }

    override fun clearUserRestriction(restriction: String): DevicePolicyResult =
        runIfDeviceOwner {
            devicePolicyManager.clearUserRestriction(adminComponent, restriction)
            DevicePolicyResult.Applied
        }

    private inline fun runIfDeviceOwner(block: () -> DevicePolicyResult): DevicePolicyResult {
        if (!isDeviceOwner()) {
            return DevicePolicyResult.DeviceOwnerMissing
        }

        return try {
            block()
        } catch (throwable: Throwable) {
            DevicePolicyResult.Failed(throwable)
        }
    }

    private companion object {
        const val RECEIVER_PACKAGE = "com.lockin.device"
    }
}

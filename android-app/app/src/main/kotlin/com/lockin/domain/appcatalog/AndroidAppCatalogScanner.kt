package com.lockin.domain.appcatalog

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidAppCatalogScanner(
    private val context: Context,
    private val packageManager: PackageManager = context.packageManager,
    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(DevicePolicyManager::class.java),
    private val adminComponent: ComponentName = ComponentName(
        context.packageName,
        "com.lockin.device.LockinDeviceAdminReceiver"
    )
) : AppCatalogScanner {
    override suspend fun scanInstalledApps(): List<AppIdentity> = withContext(Dispatchers.Default) {
        installedApplications()
            .asSequence()
            .filter { it.packageName.isNotBlank() }
            .map { applicationInfo ->
                val packageId = applicationInfo.packageName
                AppIdentity(
                    packageId = packageId,
                    displayName = applicationInfo.loadLabel(packageManager).toString(),
                    iconRef = packageId,
                    isInstalled = true,
                    isLockinApp = packageId == context.packageName,
                    isPolicyExempt = isPolicyExempt(packageId)
                )
            }
            .sortedBy { it.displayName.lowercase() }
            .toList()
    }

    private fun installedApplications(): List<ApplicationInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(0)
        }

    private fun isPolicyExempt(packageId: String): Boolean =
        packageId == context.packageName ||
            packageId == currentLauncherPackageId() ||
            isDevicePolicyPackage(packageId)

    private fun currentLauncherPackageId(): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfo?.activityInfo?.packageName
    }

    private fun isDevicePolicyPackage(packageId: String): Boolean =
        runCatching {
            devicePolicyManager.isDeviceOwnerApp(packageId) ||
                devicePolicyManager.isProfileOwnerApp(packageId) ||
                packageId == adminComponent.packageName
        }.getOrDefault(packageId == adminComponent.packageName)
}

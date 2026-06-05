package com.lockin.domain.appcatalog

data class AppIdentity(
    val packageId: String,
    val displayName: String,
    val iconRef: String?,
    val isInstalled: Boolean,
    val isLockinApp: Boolean,
    val isPolicyExempt: Boolean
)

interface AppCatalogScanner {
    suspend fun scanInstalledApps(): List<AppIdentity>
}

package com.lockin.domain.groups

data class LockGroup(
    val id: Long = 0,
    val name: String,
    val packageIds: Set<String>
) {
    companion object {
        fun validateName(name: String): String? =
            if (name.isBlank()) "Group name is required." else null

        fun validatePackageIds(packageIds: Set<String>): String? =
            if (packageIds.isEmpty()) "Select at least one app for this group." else null
    }
}

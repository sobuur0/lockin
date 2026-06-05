package com.lockin.domain.moods

import com.lockin.domain.lock.LockDuration

data class Mood(
    val id: Long = 0,
    val name: String,
    val packageIds: Set<String>,
    val defaultDuration: LockDuration? = null
) {
    companion object {
        fun validateName(name: String): String? =
            if (name.isBlank()) "Mood name is required." else null

        fun validatePackageIds(packageIds: Set<String>): String? =
            if (packageIds.isEmpty()) "Select at least one app for this mood." else null
    }
}

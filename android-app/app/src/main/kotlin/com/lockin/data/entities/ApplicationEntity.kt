package com.lockin.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "applications")
data class ApplicationEntity(
    @PrimaryKey val packageId: String,
    val displayName: String,
    val iconRef: String?,
    val isInstalled: Boolean,
    val isLockinApp: Boolean,
    val isPolicyExempt: Boolean,
    val lastSeenAt: Long
)

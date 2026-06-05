package com.lockin.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class LockStatus {
    SCHEDULED,
    ACTIVE,
    COMPLETED,
    FAILED_CLOSED
}

enum class LockSourceType {
    MANUAL,
    GROUP,
    MOOD
}

@Entity(
    tableName = "locks",
    indices = [
        Index(value = ["status"]),
        Index(value = ["committedEndWallTime"]),
        Index(value = ["sourceType", "sourceId"])
    ]
)
data class LockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val status: LockStatus,
    val createdAtWallTime: Long,
    val startedAtWallTime: Long,
    val startedAtElapsedRealtime: Long,
    val committedEndWallTime: Long,
    val remainingDurationAtLastCheckpoint: Long,
    val lastCheckpointElapsedRealtime: Long,
    val sourceType: LockSourceType,
    val sourceId: Long?,
    val confirmationTextVersion: Int
)

@Entity(
    tableName = "lock_applications",
    primaryKeys = ["lockId", "packageId"],
    foreignKeys = [
        ForeignKey(
            entity = LockEntity::class,
            parentColumns = ["id"],
            childColumns = ["lockId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ApplicationEntity::class,
            parentColumns = ["packageId"],
            childColumns = ["packageId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["packageId"]),
        Index(value = ["lockId"])
    ]
)
data class LockApplicationEntity(
    val lockId: Long,
    val packageId: String,
    val addedAt: Long
)

@Entity(
    tableName = "lock_extensions",
    foreignKeys = [
        ForeignKey(
            entity = LockEntity::class,
            parentColumns = ["id"],
            childColumns = ["lockId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["lockId"])]
)
data class LockExtensionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lockId: Long,
    val confirmedAtWallTime: Long,
    val confirmedAtElapsedRealtime: Long,
    val previousRemainingDuration: Long,
    val extensionDuration: Long,
    val resultingRemainingDuration: Long,
    val resultingEndWallTime: Long
)

package com.lockin.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PolicyReconciliationTrigger {
    APP_START,
    BOOT,
    PACKAGE_CHANGED,
    LOCK_CREATED,
    LOCK_EXTENDED,
    LOCK_EXPIRED,
    MANUAL_VERIFY
}

enum class PolicyReconciliationResult {
    APPLIED,
    ALREADY_APPLIED,
    POLICY_EXEMPT,
    DEVICE_OWNER_MISSING,
    FAILED_CLOSED
}

@Entity(
    tableName = "lock_sessions",
    foreignKeys = [
        ForeignKey(
            entity = LockEntity::class,
            parentColumns = ["id"],
            childColumns = ["lockId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["lockId"], unique = true),
        Index(value = ["sourceType", "sourceId"]),
        Index(value = ["completedAtWallTime"])
    ]
)
data class LockSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lockId: Long,
    val startedAtWallTime: Long,
    val completedAtWallTime: Long,
    val totalCommittedDuration: Long,
    val sourceType: LockSourceType,
    val sourceId: Long?
)

@Entity(
    tableName = "lock_session_applications",
    primaryKeys = ["sessionId", "packageId"],
    foreignKeys = [
        ForeignKey(
            entity = LockSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
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
        Index(value = ["sessionId"]),
        Index(value = ["packageId"])
    ]
)
data class LockSessionApplicationEntity(
    val sessionId: Long,
    val packageId: String
)

@Entity(
    tableName = "policy_reconciliation_events",
    indices = [
        Index(value = ["occurredAt"]),
        Index(value = ["trigger"]),
        Index(value = ["packageId"]),
        Index(value = ["result"])
    ]
)
data class PolicyReconciliationEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val occurredAt: Long,
    val trigger: PolicyReconciliationTrigger,
    val packageId: String?,
    val result: PolicyReconciliationResult,
    val message: String
)

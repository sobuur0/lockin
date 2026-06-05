package com.lockin.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lock_groups",
    indices = [Index(value = ["name"])]
)
data class LockGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean
)

@Entity(
    tableName = "lock_group_applications",
    primaryKeys = ["groupId", "packageId"],
    foreignKeys = [
        ForeignKey(
            entity = LockGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
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
        Index(value = ["groupId"]),
        Index(value = ["packageId"])
    ]
)
data class LockGroupApplicationEntity(
    val groupId: Long,
    val packageId: String,
    val addedAt: Long
)

@Entity(
    tableName = "moods",
    indices = [Index(value = ["name"])]
)
data class MoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val defaultDuration: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean
)

@Entity(
    tableName = "mood_applications",
    primaryKeys = ["moodId", "packageId"],
    foreignKeys = [
        ForeignKey(
            entity = MoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["moodId"],
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
        Index(value = ["moodId"]),
        Index(value = ["packageId"])
    ]
)
data class MoodApplicationEntity(
    val moodId: Long,
    val packageId: String,
    val addedAt: Long
)

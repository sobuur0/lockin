package com.lockin.data.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.lockin.data.entities.LockGroupApplicationEntity
import com.lockin.data.entities.LockGroupEntity
import com.lockin.data.entities.MoodApplicationEntity
import com.lockin.data.entities.MoodEntity
import kotlinx.coroutines.flow.Flow

data class LockGroupWithApplications(
    @Embedded val group: LockGroupEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "groupId"
    )
    val applications: List<LockGroupApplicationEntity>
)

data class MoodWithApplications(
    @Embedded val mood: MoodEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "moodId"
    )
    val applications: List<MoodApplicationEntity>
)

@Dao
interface TemplateDao {
    @Insert
    suspend fun insertGroup(group: LockGroupEntity): Long

    @Update
    suspend fun updateGroup(group: LockGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceGroupApplications(applications: List<LockGroupApplicationEntity>)

    @Query("DELETE FROM lock_group_applications WHERE groupId = :groupId")
    suspend fun deleteGroupApplications(groupId: Long)

    @Transaction
    @Query("SELECT * FROM lock_groups WHERE isArchived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActiveGroups(): Flow<List<LockGroupWithApplications>>

    @Transaction
    @Query("SELECT * FROM lock_groups WHERE id = :groupId")
    suspend fun getGroup(groupId: Long): LockGroupWithApplications?

    @Insert
    suspend fun insertMood(mood: MoodEntity): Long

    @Update
    suspend fun updateMood(mood: MoodEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceMoodApplications(applications: List<MoodApplicationEntity>)

    @Query("DELETE FROM mood_applications WHERE moodId = :moodId")
    suspend fun deleteMoodApplications(moodId: Long)

    @Transaction
    @Query("SELECT * FROM moods WHERE isArchived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActiveMoods(): Flow<List<MoodWithApplications>>

    @Transaction
    @Query("SELECT * FROM moods WHERE id = :moodId")
    suspend fun getMood(moodId: Long): MoodWithApplications?
}

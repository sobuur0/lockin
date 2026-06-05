package com.lockin.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lockin.data.entities.ApplicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApplicationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(application: ApplicationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(applications: List<ApplicationEntity>)

    @Query("SELECT * FROM applications ORDER BY displayName COLLATE NOCASE")
    fun observeAll(): Flow<List<ApplicationEntity>>

    @Query(
        """
        SELECT * FROM applications
        WHERE isInstalled = 1
            AND isLockinApp = 0
            AND isPolicyExempt = 0
        ORDER BY displayName COLLATE NOCASE
        """
    )
    fun observeLockableInstalled(): Flow<List<ApplicationEntity>>

    @Query("SELECT * FROM applications WHERE packageId = :packageId")
    suspend fun getByPackageId(packageId: String): ApplicationEntity?

    @Query(
        """
        UPDATE applications
        SET isInstalled = :isInstalled,
            lastSeenAt = :lastSeenAt
        WHERE packageId = :packageId
        """
    )
    suspend fun updateInstalledState(
        packageId: String,
        isInstalled: Boolean,
        lastSeenAt: Long
    )
}

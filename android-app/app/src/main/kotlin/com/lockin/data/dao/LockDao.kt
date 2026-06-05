package com.lockin.data.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.lockin.data.entities.LockApplicationEntity
import com.lockin.data.entities.LockEntity
import com.lockin.data.entities.LockExtensionEntity
import kotlinx.coroutines.flow.Flow

data class LockWithApplications(
    @Embedded val lock: LockEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "lockId"
    )
    val applications: List<LockApplicationEntity>
)

@Dao
interface LockDao {
    @Insert
    suspend fun insertLock(lock: LockEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLockApplications(applications: List<LockApplicationEntity>)

    @Insert
    suspend fun insertExtension(extension: LockExtensionEntity): Long

    @Update
    suspend fun updateLock(lock: LockEntity)

    @Query("SELECT * FROM locks WHERE id = :lockId")
    suspend fun getLock(lockId: Long): LockEntity?

    @Query("SELECT * FROM locks ORDER BY createdAtWallTime DESC")
    fun observeAllLocks(): Flow<List<LockEntity>>

    @Transaction
    @Query("SELECT * FROM locks WHERE id = :lockId")
    suspend fun getLockWithApplications(lockId: Long): LockWithApplications?

    @Transaction
    @Query(
        """
        SELECT * FROM locks
        WHERE status IN ('ACTIVE', 'FAILED_CLOSED')
        ORDER BY committedEndWallTime ASC
        """
    )
    fun observeActiveLocks(): Flow<List<LockWithApplications>>

    @Query(
        """
        SELECT DISTINCT packageId FROM lock_applications
        WHERE lockId IN (
            SELECT id FROM locks WHERE status IN ('ACTIVE', 'FAILED_CLOSED')
        )
        ORDER BY packageId
        """
    )
    fun observeActivePackageIds(): Flow<List<String>>

    @Query(
        """
        SELECT MAX(locks.committedEndWallTime)
        FROM locks
        INNER JOIN lock_applications ON lock_applications.lockId = locks.id
        WHERE lock_applications.packageId = :packageId
            AND locks.status IN ('ACTIVE', 'FAILED_CLOSED')
        """
    )
    suspend fun latestActiveEndForPackage(packageId: String): Long?

    @Query(
        """
        SELECT * FROM lock_extensions
        WHERE lockId = :lockId
        ORDER BY confirmedAtWallTime ASC
        """
    )
    fun observeExtensions(lockId: Long): Flow<List<LockExtensionEntity>>
}

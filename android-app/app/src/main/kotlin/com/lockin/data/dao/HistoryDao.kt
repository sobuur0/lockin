package com.lockin.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lockin.data.entities.LockSessionApplicationEntity
import com.lockin.data.entities.LockSessionEntity
import com.lockin.data.entities.PolicyReconciliationEventEntity
import kotlinx.coroutines.flow.Flow

data class ApplicationBlockCount(
    val packageId: String,
    val displayName: String,
    val blockCount: Int
)

data class MoodUsageCount(
    val moodId: Long,
    val name: String,
    val useCount: Int
)

data class StatisticsTotals(
    val totalLockDuration: Long?,
    val completedLockSessionCount: Int,
    val longestLockDuration: Long?,
    val averageLockDuration: Double?
)

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: LockSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSessionApplications(applications: List<LockSessionApplicationEntity>)

    @Insert
    suspend fun insertPolicyEvent(event: PolicyReconciliationEventEntity): Long

    @Query("SELECT * FROM lock_sessions ORDER BY completedAtWallTime DESC")
    fun observeSessions(): Flow<List<LockSessionEntity>>

    @Query(
        """
        SELECT
            SUM(totalCommittedDuration) AS totalLockDuration,
            COUNT(*) AS completedLockSessionCount,
            MAX(totalCommittedDuration) AS longestLockDuration,
            AVG(totalCommittedDuration) AS averageLockDuration
        FROM lock_sessions
        """
    )
    fun observeStatisticsTotals(): Flow<StatisticsTotals>

    @Query(
        """
        SELECT
            applications.packageId AS packageId,
            applications.displayName AS displayName,
            COUNT(lock_session_applications.packageId) AS blockCount
        FROM lock_session_applications
        INNER JOIN applications ON applications.packageId = lock_session_applications.packageId
        GROUP BY applications.packageId, applications.displayName
        ORDER BY blockCount DESC, applications.displayName COLLATE NOCASE
        """
    )
    fun observeMostBlockedApplications(): Flow<List<ApplicationBlockCount>>

    @Query("SELECT COUNT(DISTINCT packageId) FROM lock_session_applications")
    fun observeUniqueBlockedApplicationCount(): Flow<Int>

    @Query(
        """
        SELECT
            moods.id AS moodId,
            moods.name AS name,
            COUNT(lock_sessions.id) AS useCount
        FROM lock_sessions
        INNER JOIN moods ON moods.id = lock_sessions.sourceId
        WHERE lock_sessions.sourceType = 'MOOD'
        GROUP BY moods.id, moods.name
        ORDER BY useCount DESC, moods.name COLLATE NOCASE
        LIMIT 1
        """
    )
    fun observeMostFrequentlyUsedMood(): Flow<MoodUsageCount?>

    @Query("SELECT * FROM policy_reconciliation_events ORDER BY occurredAt DESC")
    fun observePolicyEvents(): Flow<List<PolicyReconciliationEventEntity>>

    @Query("SELECT * FROM policy_reconciliation_events ORDER BY occurredAt DESC LIMIT :limit")
    fun observeRecentPolicyEvents(limit: Int): Flow<List<PolicyReconciliationEventEntity>>
}

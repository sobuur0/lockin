package com.lockin.domain.repository

import com.lockin.data.dao.ApplicationBlockCount
import com.lockin.data.dao.LockGroupWithApplications
import com.lockin.data.dao.LockWithApplications
import com.lockin.data.dao.MoodUsageCount
import com.lockin.data.dao.MoodWithApplications
import com.lockin.data.dao.StatisticsTotals
import com.lockin.data.entities.ApplicationEntity
import com.lockin.data.entities.LockApplicationEntity
import com.lockin.data.entities.LockEntity
import com.lockin.data.entities.LockExtensionEntity
import com.lockin.data.entities.LockGroupApplicationEntity
import com.lockin.data.entities.LockGroupEntity
import com.lockin.data.entities.LockSessionApplicationEntity
import com.lockin.data.entities.LockSessionEntity
import com.lockin.data.entities.MoodApplicationEntity
import com.lockin.data.entities.MoodEntity
import com.lockin.data.entities.PolicyReconciliationEventEntity
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    fun observeAllApps(): Flow<List<ApplicationEntity>>
    fun observeLockableInstalledApps(): Flow<List<ApplicationEntity>>
    suspend fun upsertApps(applications: List<ApplicationEntity>)
    suspend fun getApp(packageId: String): ApplicationEntity?
    suspend fun updateInstalledState(packageId: String, isInstalled: Boolean, lastSeenAt: Long)
}

interface LockRepository {
    fun observeLocks(): Flow<List<LockEntity>>
    fun observeActiveLocks(): Flow<List<LockWithApplications>>
    fun observeActivePackageIds(): Flow<List<String>>
    suspend fun getLock(lockId: Long): LockEntity?
    suspend fun getLockWithApplications(lockId: Long): LockWithApplications?
    suspend fun insertLock(lock: LockEntity, applications: List<LockApplicationEntity>): Long
    suspend fun updateLock(lock: LockEntity)
    suspend fun addExtension(extension: LockExtensionEntity): Long
    suspend fun latestActiveEndForPackage(packageId: String): Long?
}

interface TemplateRepository {
    fun observeGroups(): Flow<List<LockGroupWithApplications>>
    fun observeMoods(): Flow<List<MoodWithApplications>>
    suspend fun getGroup(groupId: Long): LockGroupWithApplications?
    suspend fun getMood(moodId: Long): MoodWithApplications?
    suspend fun saveGroup(
        group: LockGroupEntity,
        applications: List<LockGroupApplicationEntity>
    ): Long

    suspend fun saveMood(
        mood: MoodEntity,
        applications: List<MoodApplicationEntity>
    ): Long
}

interface PolicyEventRepository {
    fun observeRecentEvents(limit: Int = 100): Flow<List<PolicyReconciliationEventEntity>>
    suspend fun record(event: PolicyReconciliationEventEntity): Long
}

interface StatisticsRepository {
    fun observeSessions(): Flow<List<LockSessionEntity>>
    fun observeTotals(): Flow<StatisticsTotals>
    fun observeMostBlockedApplications(): Flow<List<ApplicationBlockCount>>
    fun observeUniqueBlockedApplicationCount(): Flow<Int>
    fun observeMostFrequentlyUsedMood(): Flow<MoodUsageCount?>
    suspend fun insertSession(
        session: LockSessionEntity,
        applications: List<LockSessionApplicationEntity>
    ): Long
}

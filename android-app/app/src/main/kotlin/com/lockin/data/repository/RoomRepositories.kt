package com.lockin.data.repository

import androidx.room.withTransaction
import com.lockin.data.dao.ApplicationBlockCount
import com.lockin.data.dao.ApplicationDao
import com.lockin.data.dao.HistoryDao
import com.lockin.data.dao.LockDao
import com.lockin.data.dao.LockGroupWithApplications
import com.lockin.data.dao.LockWithApplications
import com.lockin.data.dao.MoodUsageCount
import com.lockin.data.dao.MoodWithApplications
import com.lockin.data.dao.StatisticsTotals
import com.lockin.data.dao.TemplateDao
import com.lockin.data.db.LockinDatabase
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
import com.lockin.domain.repository.AppRepository
import com.lockin.domain.repository.LockRepository
import com.lockin.domain.repository.PolicyEventRepository
import com.lockin.domain.repository.StatisticsRepository
import com.lockin.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow

class RoomAppRepository(
    private val applicationDao: ApplicationDao
) : AppRepository {
    override fun observeAllApps(): Flow<List<ApplicationEntity>> = applicationDao.observeAll()

    override fun observeLockableInstalledApps(): Flow<List<ApplicationEntity>> =
        applicationDao.observeLockableInstalled()

    override suspend fun upsertApps(applications: List<ApplicationEntity>) {
        applicationDao.upsertAll(applications)
    }

    override suspend fun getApp(packageId: String): ApplicationEntity? =
        applicationDao.getByPackageId(packageId)

    override suspend fun updateInstalledState(
        packageId: String,
        isInstalled: Boolean,
        lastSeenAt: Long
    ) {
        applicationDao.updateInstalledState(
            packageId = packageId,
            isInstalled = isInstalled,
            lastSeenAt = lastSeenAt
        )
    }
}

class RoomLockRepository(
    private val database: LockinDatabase,
    private val lockDao: LockDao
) : LockRepository {
    override fun observeLocks(): Flow<List<LockEntity>> = lockDao.observeAllLocks()

    override fun observeActiveLocks(): Flow<List<LockWithApplications>> =
        lockDao.observeActiveLocks()

    override fun observeActivePackageIds(): Flow<List<String>> =
        lockDao.observeActivePackageIds()

    override suspend fun getLock(lockId: Long): LockEntity? = lockDao.getLock(lockId)

    override suspend fun getLockWithApplications(lockId: Long): LockWithApplications? =
        lockDao.getLockWithApplications(lockId)

    override suspend fun insertLock(
        lock: LockEntity,
        applications: List<LockApplicationEntity>
    ): Long = database.withTransaction {
        val lockId = lockDao.insertLock(lock)
        lockDao.insertLockApplications(applications.map { it.copy(lockId = lockId) })
        lockId
    }

    override suspend fun updateLock(lock: LockEntity) {
        lockDao.updateLock(lock)
    }

    override suspend fun addExtension(extension: LockExtensionEntity): Long =
        lockDao.insertExtension(extension)

    override suspend fun latestActiveEndForPackage(packageId: String): Long? =
        lockDao.latestActiveEndForPackage(packageId)
}

class RoomTemplateRepository(
    private val database: LockinDatabase,
    private val templateDao: TemplateDao
) : TemplateRepository {
    override fun observeGroups(): Flow<List<LockGroupWithApplications>> =
        templateDao.observeActiveGroups()

    override fun observeMoods(): Flow<List<MoodWithApplications>> =
        templateDao.observeActiveMoods()

    override suspend fun getGroup(groupId: Long): LockGroupWithApplications? =
        templateDao.getGroup(groupId)

    override suspend fun getMood(moodId: Long): MoodWithApplications? =
        templateDao.getMood(moodId)

    override suspend fun saveGroup(
        group: LockGroupEntity,
        applications: List<LockGroupApplicationEntity>
    ): Long = database.withTransaction {
        val groupId = if (group.id == 0L) {
            templateDao.insertGroup(group)
        } else {
            templateDao.updateGroup(group)
            group.id
        }
        templateDao.deleteGroupApplications(groupId)
        templateDao.replaceGroupApplications(applications.map { it.copy(groupId = groupId) })
        groupId
    }

    override suspend fun saveMood(
        mood: MoodEntity,
        applications: List<MoodApplicationEntity>
    ): Long = database.withTransaction {
        val moodId = if (mood.id == 0L) {
            templateDao.insertMood(mood)
        } else {
            templateDao.updateMood(mood)
            mood.id
        }
        templateDao.deleteMoodApplications(moodId)
        templateDao.replaceMoodApplications(applications.map { it.copy(moodId = moodId) })
        moodId
    }
}

class RoomPolicyEventRepository(
    private val historyDao: HistoryDao
) : PolicyEventRepository {
    override fun observeRecentEvents(limit: Int): Flow<List<PolicyReconciliationEventEntity>> =
        historyDao.observeRecentPolicyEvents(limit)

    override suspend fun record(event: PolicyReconciliationEventEntity): Long =
        historyDao.insertPolicyEvent(event)
}

class RoomStatisticsRepository(
    private val database: LockinDatabase,
    private val historyDao: HistoryDao
) : StatisticsRepository {
    override fun observeSessions(): Flow<List<LockSessionEntity>> =
        historyDao.observeSessions()

    override fun observeTotals(): Flow<StatisticsTotals> =
        historyDao.observeStatisticsTotals()

    override fun observeMostBlockedApplications(): Flow<List<ApplicationBlockCount>> =
        historyDao.observeMostBlockedApplications()

    override fun observeUniqueBlockedApplicationCount(): Flow<Int> =
        historyDao.observeUniqueBlockedApplicationCount()

    override fun observeMostFrequentlyUsedMood(): Flow<MoodUsageCount?> =
        historyDao.observeMostFrequentlyUsedMood()

    override suspend fun insertSession(
        session: LockSessionEntity,
        applications: List<LockSessionApplicationEntity>
    ): Long = database.withTransaction {
        val sessionId = historyDao.insertSession(session)
        historyDao.insertSessionApplications(applications.map { it.copy(sessionId = sessionId) })
        sessionId
    }
}

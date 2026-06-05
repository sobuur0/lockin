package com.lockin.testsupport

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
import com.lockin.data.entities.LockStatus
import com.lockin.data.entities.MoodApplicationEntity
import com.lockin.data.entities.MoodEntity
import com.lockin.data.entities.PolicyReconciliationEventEntity
import com.lockin.data.entities.PolicyReconciliationTrigger
import com.lockin.device.ActiveLockPolicyEnforcer
import com.lockin.device.DeviceOwnerState
import com.lockin.device.DeviceOwnerStatus
import com.lockin.device.LockPolicyEnforcementSummary
import com.lockin.domain.lock.TimeProvider
import com.lockin.domain.repository.AppRepository
import com.lockin.domain.repository.LockRepository
import com.lockin.domain.repository.PolicyEventRepository
import com.lockin.domain.repository.StatisticsRepository
import com.lockin.domain.repository.TemplateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class FakeTimeProvider(
    var wallTime: Long = 1_000L,
    var elapsedTime: Long = 1_000L
) : TimeProvider {
    override fun wallTimeMillis(): Long = wallTime
    override fun elapsedRealtimeMillis(): Long = elapsedTime
}

class FakeDeviceOwnerState(
    var isDeviceOwner: Boolean = true,
    var packageName: String = "com.lockin"
) : DeviceOwnerState {
    override fun currentStatus(): DeviceOwnerStatus =
        DeviceOwnerStatus(
            isDeviceOwner = isDeviceOwner,
            packageName = packageName
        )
}

class FakePolicyEnforcer : ActiveLockPolicyEnforcer {
    val triggers = mutableListOf<PolicyReconciliationTrigger>()

    override suspend fun enforceActiveLocks(
        trigger: PolicyReconciliationTrigger
    ): LockPolicyEnforcementSummary {
        triggers += trigger
        return LockPolicyEnforcementSummary(
            trigger = trigger,
            packageCount = 0,
            blockedPackageIds = emptySet(),
            failedPackageIds = emptySet(),
            deviceOwnerMissing = false
        )
    }
}

class FakeAppRepository(
    apps: List<ApplicationEntity> = emptyList()
) : AppRepository {
    private val appsFlow = MutableStateFlow(apps)
    private val appsByPackageId = apps.associateBy { it.packageId }.toMutableMap()

    override fun observeAllApps(): Flow<List<ApplicationEntity>> = appsFlow

    override fun observeLockableInstalledApps(): Flow<List<ApplicationEntity>> = appsFlow

    override suspend fun upsertApps(applications: List<ApplicationEntity>) {
        applications.forEach { appsByPackageId[it.packageId] = it }
        appsFlow.value = appsByPackageId.values.toList()
    }

    override suspend fun getApp(packageId: String): ApplicationEntity? = appsByPackageId[packageId]

    override suspend fun updateInstalledState(
        packageId: String,
        isInstalled: Boolean,
        lastSeenAt: Long
    ) {
        appsByPackageId[packageId]?.let { app ->
            appsByPackageId[packageId] = app.copy(
                isInstalled = isInstalled,
                lastSeenAt = lastSeenAt
            )
        }
        appsFlow.value = appsByPackageId.values.toList()
    }
}

class FakeLockRepository : LockRepository {
    private val locks = mutableMapOf<Long, LockEntity>()
    private val applications = mutableMapOf<Long, List<LockApplicationEntity>>()
    val extensions = mutableListOf<LockExtensionEntity>()
    private var nextId = 1L

    override fun observeLocks(): Flow<List<LockEntity>> = MutableStateFlow(locks.values.toList())

    override fun observeActiveLocks(): Flow<List<LockWithApplications>> =
        MutableStateFlow(
            locks.values
                .filter { it.status == LockStatus.ACTIVE || it.status == LockStatus.FAILED_CLOSED }
                .map { lock ->
                    LockWithApplications(
                        lock = lock,
                        applications = applications[lock.id].orEmpty()
                    )
                }
        )

    override fun observeActivePackageIds(): Flow<List<String>> =
        MutableStateFlow(
            locks.values
                .filter { it.status == LockStatus.ACTIVE || it.status == LockStatus.FAILED_CLOSED }
                .flatMap { lock -> applications[lock.id].orEmpty() }
                .map { it.packageId }
                .distinct()
        )

    override suspend fun getLock(lockId: Long): LockEntity? = locks[lockId]

    override suspend fun getLockWithApplications(lockId: Long): LockWithApplications? =
        locks[lockId]?.let { lock ->
            LockWithApplications(lock, applications[lockId].orEmpty())
        }

    override suspend fun insertLock(
        lock: LockEntity,
        applications: List<LockApplicationEntity>
    ): Long {
        val id = nextId++
        locks[id] = lock.copy(id = id)
        this.applications[id] = applications.map { it.copy(lockId = id) }
        return id
    }

    override suspend fun updateLock(lock: LockEntity) {
        locks[lock.id] = lock
    }

    override suspend fun addExtension(extension: LockExtensionEntity): Long {
        val id = extensions.size + 1L
        extensions += extension.copy(id = id)
        return id
    }

    override suspend fun latestActiveEndForPackage(packageId: String): Long? =
        locks.values
            .filter { it.status == LockStatus.ACTIVE || it.status == LockStatus.FAILED_CLOSED }
            .filter { lock -> applications[lock.id].orEmpty().any { it.packageId == packageId } }
            .maxOfOrNull { it.committedEndWallTime }
}

class FakePolicyEventRepository : PolicyEventRepository {
    val events = mutableListOf<PolicyReconciliationEventEntity>()

    override fun observeRecentEvents(limit: Int): Flow<List<PolicyReconciliationEventEntity>> =
        MutableStateFlow(events.take(limit))

    override suspend fun record(event: PolicyReconciliationEventEntity): Long {
        val id = events.size + 1L
        events += event.copy(id = id)
        return id
    }
}

class FakeTemplateRepository : TemplateRepository {
    override fun observeGroups(): Flow<List<LockGroupWithApplications>> = emptyFlow()
    override fun observeMoods(): Flow<List<MoodWithApplications>> = emptyFlow()
    override suspend fun getGroup(groupId: Long): LockGroupWithApplications? = null
    override suspend fun getMood(moodId: Long): MoodWithApplications? = null
    override suspend fun saveGroup(
        group: LockGroupEntity,
        applications: List<LockGroupApplicationEntity>
    ): Long = group.id

    override suspend fun saveMood(
        mood: MoodEntity,
        applications: List<MoodApplicationEntity>
    ): Long = mood.id
}

class FakeStatisticsRepository : StatisticsRepository {
    override fun observeSessions(): Flow<List<LockSessionEntity>> = emptyFlow()
    override fun observeTotals(): Flow<StatisticsTotals> = emptyFlow()
    override fun observeMostBlockedApplications(): Flow<List<ApplicationBlockCount>> = emptyFlow()
    override fun observeUniqueBlockedApplicationCount(): Flow<Int> = emptyFlow()
    override fun observeMostFrequentlyUsedMood(): Flow<MoodUsageCount?> = emptyFlow()
    override suspend fun insertSession(
        session: LockSessionEntity,
        applications: List<LockSessionApplicationEntity>
    ): Long = session.id
}

fun lockableApp(packageId: String = "com.example.blocked"): ApplicationEntity =
    ApplicationEntity(
        packageId = packageId,
        displayName = "Blocked",
        iconRef = null,
        isInstalled = true,
        isLockinApp = false,
        isPolicyExempt = false,
        lastSeenAt = 1_000L
    )

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestRule {
    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                Dispatchers.setMain(dispatcher)
                try {
                    base.evaluate()
                } finally {
                    Dispatchers.resetMain()
                }
            }
        }
}

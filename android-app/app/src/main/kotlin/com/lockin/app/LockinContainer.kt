package com.lockin.app

import android.content.Context
import androidx.room.Room
import com.lockin.data.db.LockinDatabase
import com.lockin.data.repository.RoomAppRepository
import com.lockin.data.repository.RoomLockRepository
import com.lockin.data.repository.RoomPolicyEventRepository
import com.lockin.data.repository.RoomStatisticsRepository
import com.lockin.data.repository.RoomTemplateRepository
import com.lockin.device.AndroidDeviceOwnerState
import com.lockin.device.AndroidDevicePolicyGateway
import com.lockin.device.DeviceOwnerState
import com.lockin.device.DevicePolicyGateway
import com.lockin.device.LockReconciliationRunner
import com.lockin.device.LockPolicyEnforcer
import com.lockin.device.TimeRestrictionPolicy
import com.lockin.domain.appcatalog.AndroidAppCatalogScanner
import com.lockin.domain.appcatalog.AppCatalogScanner
import com.lockin.domain.lock.AndroidLockExpirationScheduler
import com.lockin.domain.lock.LockUseCases
import com.lockin.domain.lock.LockExpirationReconciler
import com.lockin.domain.lock.LockExpirationScheduler
import com.lockin.domain.lock.SystemTimeProvider
import com.lockin.domain.lock.TimeProvider
import com.lockin.domain.templates.TemplateUseCases
import com.lockin.domain.repository.AppRepository
import com.lockin.domain.repository.LockRepository
import com.lockin.domain.repository.PolicyEventRepository
import com.lockin.domain.repository.StatisticsRepository
import com.lockin.domain.repository.TemplateRepository
import com.lockin.data.entities.PolicyReconciliationTrigger
import com.lockin.domain.statistics.LockSessionRecorder
import kotlinx.coroutines.CoroutineScope

class LockinContainer(
    context: Context,
    private val applicationScope: CoroutineScope? = null
) {
    private val appContext = context.applicationContext

    val database: LockinDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            LockinDatabase::class.java,
            DATABASE_NAME
        ).build()
    }

    val timeProvider: TimeProvider = SystemTimeProvider

    val deviceOwnerState: DeviceOwnerState by lazy {
        AndroidDeviceOwnerState(appContext)
    }

    val devicePolicyGateway: DevicePolicyGateway by lazy {
        AndroidDevicePolicyGateway(appContext)
    }

    val appCatalogScanner: AppCatalogScanner by lazy {
        AndroidAppCatalogScanner(appContext)
    }

    val appRepository: AppRepository by lazy {
        RoomAppRepository(database.applicationDao())
    }

    val lockRepository: LockRepository by lazy {
        RoomLockRepository(database, database.lockDao())
    }

    val templateRepository: TemplateRepository by lazy {
        RoomTemplateRepository(database, database.templateDao())
    }

    val policyEventRepository: PolicyEventRepository by lazy {
        RoomPolicyEventRepository(database.historyDao())
    }

    val statisticsRepository: StatisticsRepository by lazy {
        RoomStatisticsRepository(database, database.historyDao())
    }

    val timeRestrictionPolicy: TimeRestrictionPolicy by lazy {
        TimeRestrictionPolicy(devicePolicyGateway)
    }

    val lockPolicyEnforcer: LockPolicyEnforcer by lazy {
        LockPolicyEnforcer(
            lockRepository = lockRepository,
            policyEventRepository = policyEventRepository,
            devicePolicyGateway = devicePolicyGateway,
            timeProvider = timeProvider,
            timeRestrictionPolicy = timeRestrictionPolicy
        )
    }

    val lockExpirationScheduler: LockExpirationScheduler by lazy {
        AndroidLockExpirationScheduler(
            context = appContext,
            lockRepository = lockRepository,
            timeProvider = timeProvider,
            coroutineScope = applicationScope,
            onExpiration = {
                lockReconciliationRunner.reconcile(
                    trigger = PolicyReconciliationTrigger.LOCK_EXPIRED,
                    refreshInstalledApps = false
                )
            }
        )
    }

    val lockUseCases: LockUseCases by lazy {
        LockUseCases(
            appRepository = appRepository,
            lockRepository = lockRepository,
            deviceOwnerState = deviceOwnerState,
            timeProvider = timeProvider,
            policyEnforcer = lockPolicyEnforcer,
            lockExpirationScheduler = lockExpirationScheduler
        )
    }

    val templateUseCases: TemplateUseCases by lazy {
        TemplateUseCases(
            appRepository = appRepository,
            templateRepository = templateRepository,
            lockUseCases = lockUseCases,
            timeProvider = timeProvider
        )
    }

    val lockExpirationReconciler: LockExpirationReconciler by lazy {
        LockExpirationReconciler(
            lockRepository = lockRepository,
            policyEnforcer = lockPolicyEnforcer,
            timeProvider = timeProvider,
            lockSessionRecorder = lockSessionRecorder
        )
    }

    val lockSessionRecorder: LockSessionRecorder by lazy {
        LockSessionRecorder(statisticsRepository)
    }

    val lockReconciliationRunner: LockReconciliationRunner by lazy {
        LockReconciliationRunner(
            appCatalogScanner = appCatalogScanner,
            appRepository = appRepository,
            lockExpirationReconciler = lockExpirationReconciler,
            lockExpirationScheduler = lockExpirationScheduler
        )
    }

    private companion object {
        const val DATABASE_NAME = "lockin.db"
    }
}

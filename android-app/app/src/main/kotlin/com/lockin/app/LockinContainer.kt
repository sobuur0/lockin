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
import com.lockin.device.LockPolicyEnforcer
import com.lockin.domain.appcatalog.AndroidAppCatalogScanner
import com.lockin.domain.appcatalog.AppCatalogScanner
import com.lockin.domain.lock.LockUseCases
import com.lockin.domain.lock.LockExpirationReconciler
import com.lockin.domain.lock.SystemTimeProvider
import com.lockin.domain.lock.TimeProvider
import com.lockin.domain.templates.TemplateUseCases
import com.lockin.domain.repository.AppRepository
import com.lockin.domain.repository.LockRepository
import com.lockin.domain.repository.PolicyEventRepository
import com.lockin.domain.repository.StatisticsRepository
import com.lockin.domain.repository.TemplateRepository

class LockinContainer(context: Context) {
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

    val lockPolicyEnforcer: LockPolicyEnforcer by lazy {
        LockPolicyEnforcer(
            lockRepository = lockRepository,
            policyEventRepository = policyEventRepository,
            devicePolicyGateway = devicePolicyGateway,
            timeProvider = timeProvider
        )
    }

    val lockUseCases: LockUseCases by lazy {
        LockUseCases(
            appRepository = appRepository,
            lockRepository = lockRepository,
            deviceOwnerState = deviceOwnerState,
            timeProvider = timeProvider,
            policyEnforcer = lockPolicyEnforcer
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
            timeProvider = timeProvider
        )
    }

    private companion object {
        const val DATABASE_NAME = "lockin.db"
    }
}

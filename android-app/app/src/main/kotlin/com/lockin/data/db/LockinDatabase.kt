package com.lockin.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.lockin.data.dao.ApplicationDao
import com.lockin.data.dao.HistoryDao
import com.lockin.data.dao.LockDao
import com.lockin.data.dao.TemplateDao
import com.lockin.data.entities.ApplicationEntity
import com.lockin.data.entities.LockApplicationEntity
import com.lockin.data.entities.LockEntity
import com.lockin.data.entities.LockExtensionEntity
import com.lockin.data.entities.LockGroupApplicationEntity
import com.lockin.data.entities.LockGroupEntity
import com.lockin.data.entities.LockSessionApplicationEntity
import com.lockin.data.entities.LockSessionEntity
import com.lockin.data.entities.LockSourceType
import com.lockin.data.entities.LockStatus
import com.lockin.data.entities.MoodApplicationEntity
import com.lockin.data.entities.MoodEntity
import com.lockin.data.entities.PolicyReconciliationEventEntity
import com.lockin.data.entities.PolicyReconciliationResult
import com.lockin.data.entities.PolicyReconciliationTrigger

@Database(
    entities = [
        ApplicationEntity::class,
        LockEntity::class,
        LockApplicationEntity::class,
        LockExtensionEntity::class,
        LockGroupEntity::class,
        LockGroupApplicationEntity::class,
        MoodEntity::class,
        MoodApplicationEntity::class,
        LockSessionEntity::class,
        LockSessionApplicationEntity::class,
        PolicyReconciliationEventEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(LockinTypeConverters::class)
abstract class LockinDatabase : RoomDatabase() {
    abstract fun applicationDao(): ApplicationDao
    abstract fun lockDao(): LockDao
    abstract fun templateDao(): TemplateDao
    abstract fun historyDao(): HistoryDao
}

class LockinTypeConverters {
    @TypeConverter
    fun lockStatusToString(value: LockStatus): String = value.name

    @TypeConverter
    fun stringToLockStatus(value: String): LockStatus = LockStatus.valueOf(value)

    @TypeConverter
    fun lockSourceTypeToString(value: LockSourceType): String = value.name

    @TypeConverter
    fun stringToLockSourceType(value: String): LockSourceType = LockSourceType.valueOf(value)

    @TypeConverter
    fun policyTriggerToString(value: PolicyReconciliationTrigger): String = value.name

    @TypeConverter
    fun stringToPolicyTrigger(value: String): PolicyReconciliationTrigger =
        PolicyReconciliationTrigger.valueOf(value)

    @TypeConverter
    fun policyResultToString(value: PolicyReconciliationResult): String = value.name

    @TypeConverter
    fun stringToPolicyResult(value: String): PolicyReconciliationResult =
        PolicyReconciliationResult.valueOf(value)
}

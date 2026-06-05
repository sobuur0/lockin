package com.lockin.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lockin.data.db.LockinDatabase
import com.lockin.data.entities.ApplicationEntity
import com.lockin.data.entities.LockApplicationEntity
import com.lockin.data.entities.LockEntity
import com.lockin.data.entities.LockSessionApplicationEntity
import com.lockin.data.entities.LockSessionEntity
import com.lockin.data.entities.LockSourceType
import com.lockin.data.entities.LockStatus
import com.lockin.data.entities.MoodApplicationEntity
import com.lockin.data.entities.MoodEntity
import com.lockin.data.repository.RoomLockRepository
import com.lockin.data.repository.RoomStatisticsRepository
import com.lockin.data.repository.RoomTemplateRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StatisticsRepositoryTest {
    private lateinit var database: LockinDatabase
    private lateinit var lockRepository: RoomLockRepository
    private lateinit var statisticsRepository: RoomStatisticsRepository
    private lateinit var templateRepository: RoomTemplateRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LockinDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        lockRepository = RoomLockRepository(database, database.lockDao())
        statisticsRepository = RoomStatisticsRepository(database, database.historyDao())
        templateRepository = RoomTemplateRepository(database, database.templateDao())
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun computesStatisticsAcrossCompletedSessionsAndMoods() = runTest {
        database.applicationDao().upsert(testApp("com.example.one", "One"))
        database.applicationDao().upsert(testApp("com.example.two", "Two"))
        val moodId = templateRepository.saveMood(
            mood = MoodEntity(
                name = "Study",
                defaultDuration = 60_000L,
                createdAt = 1_000L,
                updatedAt = 1_000L,
                isArchived = false
            ),
            applications = listOf(
                MoodApplicationEntity(
                    moodId = 0,
                    packageId = "com.example.one",
                    addedAt = 1_000L
                )
            )
        )
        val firstLockId = lockRepository.insertLock(
            lock = completedLock(duration = 60_000L, sourceType = LockSourceType.MOOD, sourceId = moodId),
            applications = listOf(lockApp("com.example.one"))
        )
        val secondLockId = lockRepository.insertLock(
            lock = completedLock(duration = 120_000L, sourceType = LockSourceType.MANUAL, sourceId = null),
            applications = listOf(lockApp("com.example.one"), lockApp("com.example.two"))
        )

        statisticsRepository.insertSession(
            session = session(
                lockId = firstLockId,
                duration = 60_000L,
                sourceType = LockSourceType.MOOD,
                sourceId = moodId
            ),
            applications = listOf(sessionApp("com.example.one"))
        )
        statisticsRepository.insertSession(
            session = session(
                lockId = secondLockId,
                duration = 120_000L,
                sourceType = LockSourceType.MANUAL,
                sourceId = null
            ),
            applications = listOf(sessionApp("com.example.one"), sessionApp("com.example.two"))
        )

        val totals = statisticsRepository.observeTotals().first()
        val mostBlocked = statisticsRepository.observeMostBlockedApplications().first()
        val uniqueCount = statisticsRepository.observeUniqueBlockedApplicationCount().first()
        val mostUsedMood = statisticsRepository.observeMostFrequentlyUsedMood().first()

        assertEquals(180_000L, totals.totalLockDuration)
        assertEquals(2, totals.completedLockSessionCount)
        assertEquals(120_000L, totals.longestLockDuration)
        assertEquals(90_000.0, totals.averageLockDuration ?: 0.0, 0.01)
        assertEquals("com.example.one", mostBlocked.first().packageId)
        assertEquals(2, mostBlocked.first().blockCount)
        assertEquals(2, uniqueCount)
        assertEquals("Study", mostUsedMood?.name)
    }

    private fun testApp(packageId: String, displayName: String): ApplicationEntity =
        ApplicationEntity(
            packageId = packageId,
            displayName = displayName,
            iconRef = null,
            isInstalled = true,
            isLockinApp = false,
            isPolicyExempt = false,
            lastSeenAt = 1_000L
        )

    private fun completedLock(
        duration: Long,
        sourceType: LockSourceType,
        sourceId: Long?
    ): LockEntity =
        LockEntity(
            status = LockStatus.COMPLETED,
            createdAtWallTime = 1_000L,
            startedAtWallTime = 1_000L,
            startedAtElapsedRealtime = 1_000L,
            committedEndWallTime = 1_000L + duration,
            remainingDurationAtLastCheckpoint = 0,
            lastCheckpointElapsedRealtime = 1_000L + duration,
            sourceType = sourceType,
            sourceId = sourceId,
            confirmationTextVersion = 1
        )

    private fun lockApp(packageId: String): LockApplicationEntity =
        LockApplicationEntity(
            lockId = 0,
            packageId = packageId,
            addedAt = 1_000L
        )

    private fun session(
        lockId: Long,
        duration: Long,
        sourceType: LockSourceType,
        sourceId: Long?
    ): LockSessionEntity =
        LockSessionEntity(
            lockId = lockId,
            startedAtWallTime = 1_000L,
            completedAtWallTime = 1_000L + duration,
            totalCommittedDuration = duration,
            sourceType = sourceType,
            sourceId = sourceId
        )

    private fun sessionApp(packageId: String): LockSessionApplicationEntity =
        LockSessionApplicationEntity(
            sessionId = 0,
            packageId = packageId
        )
}

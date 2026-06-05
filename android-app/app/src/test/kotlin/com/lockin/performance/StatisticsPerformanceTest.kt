package com.lockin.performance

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lockin.data.db.LockinDatabase
import com.lockin.data.repository.RoomStatisticsRepository
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StatisticsPerformanceTest {
    private lateinit var database: LockinDatabase
    private lateinit var statisticsRepository: RoomStatisticsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LockinDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        statisticsRepository = RoomStatisticsRepository(database, database.historyDao())
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun statisticsQueriesCompleteQuicklyForTenThousandCompletedSessions() = runTest {
        seedCompletedSessions(count = 10_000, appCount = 25)

        val elapsedMillis = measureTimeMillis {
            val totals = statisticsRepository.observeTotals().first()
            val mostBlocked = statisticsRepository.observeMostBlockedApplications().first()
            val uniqueCount = statisticsRepository.observeUniqueBlockedApplicationCount().first()
            val mostUsedMood = statisticsRepository.observeMostFrequentlyUsedMood().first()

            assertEquals(10_000, totals.completedLockSessionCount)
            assertEquals(25, uniqueCount)
            assertEquals(25, mostBlocked.size)
            assertEquals(null, mostUsedMood)
        }

        assertTrue(
            "Statistics queries over 10,000 completed sessions took ${elapsedMillis}ms",
            elapsedMillis < 1_000L
        )
    }

    private fun seedCompletedSessions(count: Int, appCount: Int) {
        val db = database.openHelper.writableDatabase
        val insertApplication = db.compileStatement(
            """
            INSERT INTO applications (
                packageId,
                displayName,
                iconRef,
                isInstalled,
                isLockinApp,
                isPolicyExempt,
                lastSeenAt
            ) VALUES (?, ?, NULL, 1, 0, 0, ?)
            """.trimIndent()
        )
        val insertLock = db.compileStatement(
            """
            INSERT INTO locks (
                id,
                status,
                createdAtWallTime,
                startedAtWallTime,
                startedAtElapsedRealtime,
                committedEndWallTime,
                remainingDurationAtLastCheckpoint,
                lastCheckpointElapsedRealtime,
                sourceType,
                sourceId,
                confirmationTextVersion
            ) VALUES (?, 'COMPLETED', ?, ?, ?, ?, 0, ?, 'MANUAL', NULL, 1)
            """.trimIndent()
        )
        val insertSession = db.compileStatement(
            """
            INSERT INTO lock_sessions (
                id,
                lockId,
                startedAtWallTime,
                completedAtWallTime,
                totalCommittedDuration,
                sourceType,
                sourceId
            ) VALUES (?, ?, ?, ?, ?, 'MANUAL', NULL)
            """.trimIndent()
        )
        val insertSessionApplication = db.compileStatement(
            """
            INSERT INTO lock_session_applications (
                sessionId,
                packageId
            ) VALUES (?, ?)
            """.trimIndent()
        )

        db.beginTransaction()
        try {
            repeat(appCount) { index ->
                insertApplication.clearBindings()
                insertApplication.bindString(1, "com.example.app$index")
                insertApplication.bindString(2, "App $index")
                insertApplication.bindLong(3, 1_000L)
                insertApplication.executeInsert()
            }

            repeat(count) { index ->
                val id = index + 1L
                val startedAt = 1_000L + index
                val duration = 60_000L + (index % 120) * 1_000L
                val completedAt = startedAt + duration
                val packageId = "com.example.app${index % appCount}"

                insertLock.clearBindings()
                insertLock.bindLong(1, id)
                insertLock.bindLong(2, startedAt)
                insertLock.bindLong(3, startedAt)
                insertLock.bindLong(4, startedAt)
                insertLock.bindLong(5, completedAt)
                insertLock.bindLong(6, completedAt)
                insertLock.executeInsert()

                insertSession.clearBindings()
                insertSession.bindLong(1, id)
                insertSession.bindLong(2, id)
                insertSession.bindLong(3, startedAt)
                insertSession.bindLong(4, completedAt)
                insertSession.bindLong(5, duration)
                insertSession.executeInsert()

                insertSessionApplication.clearBindings()
                insertSessionApplication.bindLong(1, id)
                insertSessionApplication.bindString(2, packageId)
                insertSessionApplication.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}

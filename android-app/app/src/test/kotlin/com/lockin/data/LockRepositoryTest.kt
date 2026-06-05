package com.lockin.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lockin.data.db.LockinDatabase
import com.lockin.data.entities.ApplicationEntity
import com.lockin.data.entities.LockApplicationEntity
import com.lockin.data.entities.LockEntity
import com.lockin.data.entities.LockExtensionEntity
import com.lockin.data.entities.LockSourceType
import com.lockin.data.entities.LockStatus
import com.lockin.data.repository.RoomLockRepository
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
class LockRepositoryTest {
    private lateinit var database: LockinDatabase
    private lateinit var repository: RoomLockRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LockinDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomLockRepository(database, database.lockDao())
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun persistsLockApplicationsAndExtensions() = runTest {
        database.applicationDao().upsert(testApp("com.example.blocked"))

        val lockId = repository.insertLock(
            lock = testLock(endWallTime = 10_000L),
            applications = listOf(
                LockApplicationEntity(
                    lockId = 0,
                    packageId = "com.example.blocked",
                    addedAt = 1_000L
                )
            )
        )
        repository.addExtension(
            LockExtensionEntity(
                lockId = lockId,
                confirmedAtWallTime = 2_000L,
                confirmedAtElapsedRealtime = 2_000L,
                previousRemainingDuration = 8_000L,
                extensionDuration = 5_000L,
                resultingRemainingDuration = 13_000L,
                resultingEndWallTime = 15_000L
            )
        )

        val lock = repository.getLockWithApplications(lockId)
        val extensions = database.lockDao().observeExtensions(lockId).first()

        assertEquals(1, lock?.applications?.size)
        assertEquals("com.example.blocked", lock?.applications?.single()?.packageId)
        assertEquals(1, extensions.size)
        assertEquals(13_000L, extensions.single().resultingRemainingDuration)
    }

    @Test
    fun latestActiveEndForPackageUsesOverlappingLockWithLatestEndTime() = runTest {
        database.applicationDao().upsert(testApp("com.example.blocked"))
        repository.insertLock(
            lock = testLock(endWallTime = 10_000L),
            applications = listOf(lockApp("com.example.blocked"))
        )
        repository.insertLock(
            lock = testLock(endWallTime = 20_000L),
            applications = listOf(lockApp("com.example.blocked"))
        )

        assertEquals(
            20_000L,
            repository.latestActiveEndForPackage("com.example.blocked")
        )
    }

    private fun testApp(packageId: String): ApplicationEntity =
        ApplicationEntity(
            packageId = packageId,
            displayName = "Blocked",
            iconRef = null,
            isInstalled = true,
            isLockinApp = false,
            isPolicyExempt = false,
            lastSeenAt = 1_000L
        )

    private fun testLock(endWallTime: Long): LockEntity =
        LockEntity(
            status = LockStatus.ACTIVE,
            createdAtWallTime = 1_000L,
            startedAtWallTime = 1_000L,
            startedAtElapsedRealtime = 1_000L,
            committedEndWallTime = endWallTime,
            remainingDurationAtLastCheckpoint = endWallTime - 1_000L,
            lastCheckpointElapsedRealtime = 1_000L,
            sourceType = LockSourceType.MANUAL,
            sourceId = null,
            confirmationTextVersion = 1
        )

    private fun lockApp(packageId: String): LockApplicationEntity =
        LockApplicationEntity(
            lockId = 0,
            packageId = packageId,
            addedAt = 1_000L
        )
}

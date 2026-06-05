package com.lockin.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lockin.data.db.LockinDatabase
import com.lockin.data.entities.ApplicationEntity
import com.lockin.data.entities.LockGroupApplicationEntity
import com.lockin.data.entities.LockGroupEntity
import com.lockin.data.entities.MoodApplicationEntity
import com.lockin.data.entities.MoodEntity
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
class TemplateRepositoryTest {
    private lateinit var database: LockinDatabase
    private lateinit var repository: RoomTemplateRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LockinDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomTemplateRepository(database, database.templateDao())
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun persistsGroupWithUniqueAppMembership() = runTest {
        database.applicationDao().upsert(testApp("com.example.one"))
        database.applicationDao().upsert(testApp("com.example.two"))

        val groupId = repository.saveGroup(
            group = LockGroupEntity(
                name = "Deep Work",
                createdAt = 1_000L,
                updatedAt = 1_000L,
                isArchived = false
            ),
            applications = listOf(
                groupApp("com.example.one"),
                groupApp("com.example.two")
            )
        )

        val group = repository.getGroup(groupId)

        assertEquals("Deep Work", group?.group?.name)
        assertEquals(
            listOf("com.example.one", "com.example.two"),
            group?.applications?.map { it.packageId }?.sorted()
        )
    }

    @Test
    fun replacingGroupMembershipDoesNotDuplicateApplications() = runTest {
        database.applicationDao().upsert(testApp("com.example.one"))
        database.applicationDao().upsert(testApp("com.example.two"))

        val groupId = repository.saveGroup(
            group = LockGroupEntity(
                name = "Deep Work",
                createdAt = 1_000L,
                updatedAt = 1_000L,
                isArchived = false
            ),
            applications = listOf(groupApp("com.example.one"))
        )
        repository.saveGroup(
            group = LockGroupEntity(
                id = groupId,
                name = "Deep Work",
                createdAt = 1_000L,
                updatedAt = 2_000L,
                isArchived = false
            ),
            applications = listOf(
                groupApp("com.example.two"),
                groupApp("com.example.two")
            ).distinctBy { it.packageId }
        )

        val group = repository.getGroup(groupId)

        assertEquals(listOf("com.example.two"), group?.applications?.map { it.packageId })
    }

    @Test
    fun persistsMoodWithDefaultDurationAndUniqueMembership() = runTest {
        database.applicationDao().upsert(testApp("com.example.one"))

        val moodId = repository.saveMood(
            mood = MoodEntity(
                name = "Study",
                defaultDuration = 30_000L,
                createdAt = 1_000L,
                updatedAt = 1_000L,
                isArchived = false
            ),
            applications = listOf(moodApp("com.example.one"))
        )

        val mood = repository.getMood(moodId)
        val activeMoods = repository.observeMoods().first()

        assertEquals("Study", mood?.mood?.name)
        assertEquals(30_000L, mood?.mood?.defaultDuration)
        assertEquals(1, mood?.applications?.size)
        assertEquals(1, activeMoods.size)
    }

    private fun testApp(packageId: String): ApplicationEntity =
        ApplicationEntity(
            packageId = packageId,
            displayName = packageId,
            iconRef = null,
            isInstalled = true,
            isLockinApp = false,
            isPolicyExempt = false,
            lastSeenAt = 1_000L
        )

    private fun groupApp(packageId: String): LockGroupApplicationEntity =
        LockGroupApplicationEntity(
            groupId = 0,
            packageId = packageId,
            addedAt = 1_000L
        )

    private fun moodApp(packageId: String): MoodApplicationEntity =
        MoodApplicationEntity(
            moodId = 0,
            packageId = packageId,
            addedAt = 1_000L
        )
}

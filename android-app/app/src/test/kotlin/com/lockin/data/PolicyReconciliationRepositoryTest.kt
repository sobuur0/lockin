package com.lockin.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lockin.data.db.LockinDatabase
import com.lockin.data.entities.PolicyReconciliationEventEntity
import com.lockin.data.entities.PolicyReconciliationResult
import com.lockin.data.entities.PolicyReconciliationTrigger
import com.lockin.data.repository.RoomPolicyEventRepository
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
class PolicyReconciliationRepositoryTest {
    private lateinit var database: LockinDatabase
    private lateinit var repository: RoomPolicyEventRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LockinDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomPolicyEventRepository(database.historyDao())
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun recordsAndObservesRecentPolicyEvents() = runTest {
        repository.record(
            PolicyReconciliationEventEntity(
                occurredAt = 1_000L,
                trigger = PolicyReconciliationTrigger.APP_START,
                packageId = "com.example.blocked",
                result = PolicyReconciliationResult.APPLIED,
                message = "Applied"
            )
        )
        repository.record(
            PolicyReconciliationEventEntity(
                occurredAt = 2_000L,
                trigger = PolicyReconciliationTrigger.PACKAGE_CHANGED,
                packageId = "com.example.blocked",
                result = PolicyReconciliationResult.FAILED_CLOSED,
                message = "Failed"
            )
        )

        val events = repository.observeRecentEvents(limit = 1).first()

        assertEquals(1, events.size)
        assertEquals(PolicyReconciliationTrigger.PACKAGE_CHANGED, events.single().trigger)
        assertEquals(PolicyReconciliationResult.FAILED_CLOSED, events.single().result)
    }
}

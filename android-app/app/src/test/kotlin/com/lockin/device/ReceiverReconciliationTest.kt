package com.lockin.device

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.lockin.data.entities.PolicyReconciliationTrigger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReceiverReconciliationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        ReconciliationDispatcher.testHook = null
    }

    @Test
    fun bootReceiverDispatchesBootReconciliation() {
        val calls = mutableListOf<Pair<PolicyReconciliationTrigger, Boolean>>()
        ReconciliationDispatcher.testHook = { _, trigger, refreshInstalledApps ->
            calls += trigger to refreshInstalledApps
        }

        BootReceiver().onReceive(
            context,
            Intent(Intent.ACTION_BOOT_COMPLETED)
        )

        assertEquals(listOf(PolicyReconciliationTrigger.BOOT to true), calls)
    }

    @Test
    fun packageChangeReceiverDispatchesPackageReconciliation() {
        val calls = mutableListOf<Pair<PolicyReconciliationTrigger, Boolean>>()
        ReconciliationDispatcher.testHook = { _, trigger, refreshInstalledApps ->
            calls += trigger to refreshInstalledApps
        }

        PackageChangeReceiver().onReceive(
            context,
            Intent(Intent.ACTION_PACKAGE_REPLACED)
        )

        assertEquals(listOf(PolicyReconciliationTrigger.PACKAGE_CHANGED to true), calls)
    }

    @Test
    fun expirationReceiverDispatchesLockExpiredReconciliation() {
        val calls = mutableListOf<Pair<PolicyReconciliationTrigger, Boolean>>()
        ReconciliationDispatcher.testHook = { _, trigger, refreshInstalledApps ->
            calls += trigger to refreshInstalledApps
        }

        LockExpirationReceiver().onReceive(
            context,
            Intent(PolicyReconciliation.ACTION_EXPIRE_LOCKS)
        )

        assertEquals(listOf(PolicyReconciliationTrigger.LOCK_EXPIRED to false), calls)
    }
}

package com.lockin.device

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DevicePolicyGatewayInstrumentedTest {
    @Test
    fun deviceOwnerCanCallPackageSuspensionGateway() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val gateway = AndroidDevicePolicyGateway(context)
        assumeTrue("Device Owner managed-device test only.", gateway.isDeviceOwner())

        val result = gateway.setPackagesSuspended(emptySet(), suspended = true)

        assertEquals(emptySet<String>(), result.failedPackageIds)
    }

    @Test
    fun deviceOwnerCanToggleTimeRestrictionGateway() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val gateway = AndroidDevicePolicyGateway(context)
        assumeTrue("Device Owner managed-device test only.", gateway.isDeviceOwner())

        assertEquals(
            DevicePolicyResult.Applied,
            TimeRestrictionPolicy(gateway).reconcile(activeLocksExist = true)
        )
        assertEquals(
            DevicePolicyResult.Applied,
            TimeRestrictionPolicy(gateway).reconcile(activeLocksExist = false)
        )
    }
}

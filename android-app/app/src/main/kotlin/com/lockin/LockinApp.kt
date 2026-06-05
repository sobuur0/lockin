package com.lockin

import android.app.Application
import com.lockin.app.LockinContainer
import com.lockin.data.entities.PolicyReconciliationTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LockinApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val container: LockinContainer by lazy {
        LockinContainer(this, applicationScope)
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            container.lockReconciliationRunner.reconcile(
                trigger = PolicyReconciliationTrigger.APP_START,
                refreshInstalledApps = true
            )
        }
    }
}

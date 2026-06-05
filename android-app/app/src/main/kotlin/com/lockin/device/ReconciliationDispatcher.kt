package com.lockin.device

import android.content.BroadcastReceiver
import android.content.Context
import com.lockin.LockinApp
import com.lockin.data.entities.PolicyReconciliationTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object ReconciliationDispatcher {
    var testHook: ((Context, PolicyReconciliationTrigger, Boolean) -> Unit)? = null

    fun dispatch(
        receiver: BroadcastReceiver,
        context: Context,
        trigger: PolicyReconciliationTrigger,
        refreshInstalledApps: Boolean
    ) {
        testHook?.let { hook ->
            hook(context, trigger, refreshInstalledApps)
            return
        }

        val pendingResult = receiver.goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val app = context.applicationContext as LockinApp
                app.container.lockReconciliationRunner.reconcile(
                    trigger = trigger,
                    refreshInstalledApps = refreshInstalledApps
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}

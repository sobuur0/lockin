package com.lockin.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val trigger = PolicyReconciliation.triggerForBootIntent(intent) ?: return
        ReconciliationDispatcher.dispatch(
            receiver = this,
            context = context,
            trigger = trigger,
            refreshInstalledApps = true
        )
    }
}

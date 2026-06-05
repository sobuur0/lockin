package com.lockin.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val trigger = PolicyReconciliation.triggerForPackageIntent(intent) ?: return
        ReconciliationDispatcher.dispatch(
            receiver = this,
            context = context,
            trigger = trigger,
            refreshInstalledApps = true
        )
    }
}

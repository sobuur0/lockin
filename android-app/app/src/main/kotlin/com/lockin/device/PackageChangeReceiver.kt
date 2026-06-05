package com.lockin.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action != Intent.ACTION_PACKAGE_ADDED &&
            intent.action != Intent.ACTION_PACKAGE_CHANGED &&
            intent.action != Intent.ACTION_PACKAGE_REMOVED &&
            intent.action != Intent.ACTION_PACKAGE_REPLACED
        ) {
            return
        }
        val trigger = PolicyReconciliation.triggerForPackageIntent(intent) ?: return
        ReconciliationDispatcher.dispatch(
            receiver = this,
            context = context,
            trigger = trigger,
            refreshInstalledApps = true
        )
    }
}

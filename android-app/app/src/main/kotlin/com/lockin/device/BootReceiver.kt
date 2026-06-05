package com.lockin.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        val trigger = PolicyReconciliation.triggerForBootIntent(intent) ?: return
        ReconciliationDispatcher.dispatch(
            receiver = this,
            context = context,
            trigger = trigger,
            refreshInstalledApps = true
        )
    }
}

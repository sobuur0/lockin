package com.lockin.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lockin.data.entities.PolicyReconciliationTrigger

class LockExpirationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PolicyReconciliation.ACTION_EXPIRE_LOCKS) {
            return
        }
        ReconciliationDispatcher.dispatch(
            receiver = this,
            context = context,
            trigger = PolicyReconciliationTrigger.LOCK_EXPIRED,
            refreshInstalledApps = false
        )
    }
}

package com.lockin.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Full boot reconciliation is implemented in the US3 lifecycle tasks.
    }
}

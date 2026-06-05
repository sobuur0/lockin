package com.lockin.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Full package reconciliation is implemented in the US3 lifecycle tasks.
    }
}

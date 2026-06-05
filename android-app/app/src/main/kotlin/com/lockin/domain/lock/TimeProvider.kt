package com.lockin.domain.lock

import android.os.SystemClock

interface TimeProvider {
    fun wallTimeMillis(): Long
    fun elapsedRealtimeMillis(): Long
}

object SystemTimeProvider : TimeProvider {
    override fun wallTimeMillis(): Long = System.currentTimeMillis()

    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()
}

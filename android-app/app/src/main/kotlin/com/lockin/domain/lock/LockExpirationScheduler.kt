package com.lockin.domain.lock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.lockin.device.LockExpirationReceiver
import com.lockin.device.PolicyReconciliation
import com.lockin.domain.repository.LockRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

interface LockExpirationScheduler {
    suspend fun scheduleNextExpiration()
    fun cancel()
}

class AndroidLockExpirationScheduler(
    private val context: Context,
    private val lockRepository: LockRepository,
    private val timeProvider: TimeProvider,
    private val coroutineScope: CoroutineScope? = null,
    private val onExpiration: (suspend () -> Unit)? = null,
    private val alarmManager: AlarmManager =
        context.getSystemService(AlarmManager::class.java)
) : LockExpirationScheduler {
    private var inProcessTimer: Job? = null

    override suspend fun scheduleNextExpiration() {
        val nowElapsed = timeProvider.elapsedRealtimeMillis()
        val nowWall = timeProvider.wallTimeMillis()
        val nextDelayMillis = lockRepository.observeActiveLocks()
            .first()
            .minOfOrNull { lockWithApplications ->
                LockTiming.remainingDuration(
                    lock = lockWithApplications.lock,
                    nowElapsedRealtime = nowElapsed,
                    nowWallTime = nowWall
                ).millis
            }

        if (nextDelayMillis == null) {
            cancel()
            return
        }

        val triggerAtElapsed = nowElapsed + nextDelayMillis.coerceAtLeast(0)
        scheduleAlarm(triggerAtElapsed)
        scheduleInProcessTimer(nextDelayMillis)
    }

    override fun cancel() {
        inProcessTimer?.cancel()
        inProcessTimer = null
        alarmManager.cancel(pendingIntent())
    }

    private fun scheduleAlarm(triggerAtElapsed: Long) {
        if (canScheduleExactAlarm()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtElapsed,
                pendingIntent()
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtElapsed,
                pendingIntent()
            )
        }
    }

    private fun scheduleInProcessTimer(delayMillis: Long) {
        inProcessTimer?.cancel()
        val scope = coroutineScope ?: return
        val callback = onExpiration ?: return
        inProcessTimer = scope.launch {
            delay(delayMillis.coerceAtLeast(0))
            callback()
        }
    }

    private fun canScheduleExactAlarm(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun pendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, LockExpirationReceiver::class.java).apply {
                action = PolicyReconciliation.ACTION_EXPIRE_LOCKS
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private companion object {
        const val REQUEST_CODE = 1001
    }
}

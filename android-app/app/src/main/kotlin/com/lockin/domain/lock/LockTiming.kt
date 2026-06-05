package com.lockin.domain.lock

import com.lockin.data.entities.LockEntity
import com.lockin.data.entities.LockExtensionEntity

data class RemainingDuration(
    val millis: Long
) {
    val isElapsed: Boolean = millis <= 0
}

data class LockCheckpoint(
    val remainingDurationMillis: Long,
    val checkpointElapsedRealtime: Long
)

data class LockExtensionCalculation(
    val previousRemainingDuration: Long,
    val extensionDuration: Long,
    val resultingRemainingDuration: Long,
    val resultingEndWallTime: Long
)

object LockTiming {
    fun committedEndWallTime(startedAtWallTime: Long, duration: LockDuration): Long =
        startedAtWallTime + duration.millis

    fun remainingDuration(
        remainingAtLastCheckpoint: Long,
        lastCheckpointElapsedRealtime: Long,
        nowElapsedRealtime: Long
    ): RemainingDuration {
        val elapsedSinceCheckpoint = (nowElapsedRealtime - lastCheckpointElapsedRealtime)
            .coerceAtLeast(0)
        return RemainingDuration(
            millis = (remainingAtLastCheckpoint - elapsedSinceCheckpoint).coerceAtLeast(0)
        )
    }

    fun remainingDuration(lock: LockEntity, nowElapsedRealtime: Long): RemainingDuration =
        remainingDuration(
            remainingAtLastCheckpoint = lock.remainingDurationAtLastCheckpoint,
            lastCheckpointElapsedRealtime = lock.lastCheckpointElapsedRealtime,
            nowElapsedRealtime = nowElapsedRealtime
        )

    fun checkpoint(lock: LockEntity, nowElapsedRealtime: Long): LockCheckpoint {
        val remaining = remainingDuration(lock, nowElapsedRealtime)
        return LockCheckpoint(
            remainingDurationMillis = remaining.millis,
            checkpointElapsedRealtime = nowElapsedRealtime
        )
    }

    fun calculateExtension(
        currentRemainingMillis: Long,
        extension: LockDuration,
        nowWallTime: Long
    ): LockExtensionCalculation {
        val previousRemaining = currentRemainingMillis.coerceAtLeast(0)
        val resultingRemaining = previousRemaining + extension.millis
        return LockExtensionCalculation(
            previousRemainingDuration = previousRemaining,
            extensionDuration = extension.millis,
            resultingRemainingDuration = resultingRemaining,
            resultingEndWallTime = nowWallTime + resultingRemaining
        )
    }

    fun extensionEntity(
        lockId: Long,
        calculation: LockExtensionCalculation,
        confirmedAtWallTime: Long,
        confirmedAtElapsedRealtime: Long
    ): LockExtensionEntity =
        LockExtensionEntity(
            lockId = lockId,
            confirmedAtWallTime = confirmedAtWallTime,
            confirmedAtElapsedRealtime = confirmedAtElapsedRealtime,
            previousRemainingDuration = calculation.previousRemainingDuration,
            extensionDuration = calculation.extensionDuration,
            resultingRemainingDuration = calculation.resultingRemainingDuration,
            resultingEndWallTime = calculation.resultingEndWallTime
        )
}

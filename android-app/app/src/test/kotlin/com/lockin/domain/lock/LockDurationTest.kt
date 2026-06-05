package com.lockin.domain.lock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LockDurationTest {
    @Test
    fun rejectsNonPositiveDurations() {
        assertThrows(IllegalArgumentException::class.java) {
            LockDuration.fromMillis(0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LockDuration.fromMinutes(-1)
        }
    }

    @Test
    fun convertsSupportedUnitsToMillis() {
        assertEquals(60_000L, LockDuration.fromMinutes(1).millis)
        assertEquals(3_600_000L, LockDuration.fromHours(1).millis)
        assertEquals(86_400_000L, LockDuration.fromDays(1).millis)
        assertEquals(604_800_000L, LockDuration.fromWeeks(1).millis)
    }

    @Test
    fun extensionMathAddsNewDurationToCurrentRemainingDuration() {
        val calculation = LockTiming.calculateExtension(
            currentRemainingMillis = 60_000L,
            extension = LockDuration.fromHours(5),
            nowWallTime = 1_000L
        )

        assertEquals(60_000L, calculation.previousRemainingDuration)
        assertEquals(18_000_000L, calculation.extensionDuration)
        assertEquals(18_060_000L, calculation.resultingRemainingDuration)
        assertEquals(18_061_000L, calculation.resultingEndWallTime)
    }

    @Test
    fun remainingDurationUsesElapsedRealtimeCheckpoint() {
        val remaining = LockTiming.remainingDuration(
            remainingAtLastCheckpoint = 10_000L,
            lastCheckpointElapsedRealtime = 1_000L,
            nowElapsedRealtime = 4_000L
        )

        assertEquals(7_000L, remaining.millis)
    }
}

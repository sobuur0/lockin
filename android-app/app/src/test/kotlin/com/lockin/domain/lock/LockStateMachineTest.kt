package com.lockin.domain.lock

import com.lockin.data.entities.LockStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LockStateMachineTest {
    @Test
    fun activeLockCanCompleteOnlyAfterRemainingDurationElapses() {
        val rejected = LockStateMachine.complete(
            currentStatus = LockStatus.ACTIVE,
            remainingMillis = 1
        )
        val allowed = LockStateMachine.complete(
            currentStatus = LockStatus.ACTIVE,
            remainingMillis = 0
        )

        assertTrue(rejected is LockTransitionResult.Rejected)
        assertEquals(
            LockTransitionResult.Allowed(LockStatus.COMPLETED),
            allowed
        )
    }

    @Test
    fun failedClosedLockCanRecoverToActive() {
        assertEquals(
            LockTransitionResult.Allowed(LockStatus.ACTIVE),
            LockStateMachine.activate(LockStatus.FAILED_CLOSED)
        )
    }

    @Test
    fun weakeningActionsAreAlwaysRejected() {
        LockWeakeningAction.entries.forEach { action ->
            val result = LockStateMachine.rejectWeakening(action)

            assertTrue(result.reason.contains("cannot"))
        }
    }
}

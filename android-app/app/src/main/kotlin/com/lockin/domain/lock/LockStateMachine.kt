package com.lockin.domain.lock

import com.lockin.data.entities.LockStatus

enum class LockWeakeningAction {
    REDUCE_DURATION,
    PAUSE,
    DELETE,
    BYPASS
}

sealed interface LockTransitionResult {
    data class Allowed(val nextStatus: LockStatus) : LockTransitionResult
    data class Rejected(val reason: String) : LockTransitionResult
}

object LockStateMachine {
    fun activate(currentStatus: LockStatus): LockTransitionResult =
        when (currentStatus) {
            LockStatus.SCHEDULED,
            LockStatus.FAILED_CLOSED -> LockTransitionResult.Allowed(LockStatus.ACTIVE)
            LockStatus.ACTIVE -> LockTransitionResult.Allowed(LockStatus.ACTIVE)
            LockStatus.COMPLETED -> LockTransitionResult.Rejected("Completed locks cannot be reactivated.")
        }

    fun complete(currentStatus: LockStatus, remainingMillis: Long): LockTransitionResult =
        when {
            remainingMillis > 0 -> LockTransitionResult.Rejected("Lock duration has not fully elapsed.")
            currentStatus == LockStatus.ACTIVE ||
                currentStatus == LockStatus.FAILED_CLOSED -> {
                LockTransitionResult.Allowed(LockStatus.COMPLETED)
            }
            currentStatus == LockStatus.COMPLETED -> LockTransitionResult.Allowed(LockStatus.COMPLETED)
            else -> LockTransitionResult.Rejected("Only active locks can complete.")
        }

    fun failClosed(currentStatus: LockStatus): LockTransitionResult =
        when (currentStatus) {
            LockStatus.ACTIVE,
            LockStatus.FAILED_CLOSED -> LockTransitionResult.Allowed(LockStatus.FAILED_CLOSED)
            LockStatus.SCHEDULED -> LockTransitionResult.Allowed(LockStatus.FAILED_CLOSED)
            LockStatus.COMPLETED -> LockTransitionResult.Rejected("Completed locks cannot fail closed.")
        }

    fun rejectWeakening(action: LockWeakeningAction): LockTransitionResult.Rejected =
        LockTransitionResult.Rejected(
            "Active locks cannot ${action.userFacingVerb()}."
        )

    private fun LockWeakeningAction.userFacingVerb(): String =
        when (this) {
            LockWeakeningAction.REDUCE_DURATION -> "be reduced"
            LockWeakeningAction.PAUSE -> "be paused"
            LockWeakeningAction.DELETE -> "be deleted"
            LockWeakeningAction.BYPASS -> "be bypassed"
        }
}

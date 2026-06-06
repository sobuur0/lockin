package com.lockin.ui.lockdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.data.entities.LockStatus
import com.lockin.domain.lock.LockDuration
import com.lockin.domain.lock.LockDurationUnit
import com.lockin.domain.lock.LockTiming
import com.lockin.domain.lock.LockUseCaseResult
import com.lockin.domain.lock.LockUseCases
import com.lockin.domain.lock.LockWeakeningAction
import com.lockin.domain.lock.LockStateMachine
import com.lockin.domain.lock.TimeProvider
import com.lockin.domain.repository.AppRepository
import com.lockin.domain.repository.LockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LockDetailUiState(
    val lockId: Long,
    val status: LockStatus? = null,
    val remainingMillis: Long = 0,
    val committedEndWallTime: Long = 0,
    val blockedApps: List<String> = emptyList(),
    val extensionAmount: String = "",
    val extensionUnit: LockDurationUnit = LockDurationUnit.HOURS,
    val extensionConfirmed: Boolean = false,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val rejectedActionMessage: String? = null
) {
    val remainingLabel: String = formatDuration(remainingMillis)
    val canExtend: Boolean =
        extensionAmount.toLongOrNull()?.let { it > 0 } == true &&
            extensionConfirmed &&
            !isSubmitting &&
            status != LockStatus.COMPLETED

    private companion object {
        fun formatDuration(millis: Long): String {
            val totalMinutes = (millis / 60_000L).coerceAtLeast(0)
            val seconds = ((millis / 1_000L) % 60L).coerceAtLeast(0)
            val days = totalMinutes / (24L * 60L)
            val hours = (totalMinutes % (24L * 60L)) / 60L
            val minutes = totalMinutes % 60L
            return when {
                days > 0 -> "${days}d ${hours}h"
                hours > 0 -> "${hours}h ${minutes}m"
                else -> "${minutes}m ${seconds}s"
            }
        }
    }
}

class LockDetailViewModel(
    private val lockId: Long,
    private val lockRepository: LockRepository,
    private val appRepository: AppRepository,
    private val lockUseCases: LockUseCases,
    private val timeProvider: TimeProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow(LockDetailUiState(lockId = lockId))
    val uiState: StateFlow<LockDetailUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshSnapshot()
        }
    }

    private suspend fun refreshSnapshot() {
        val lockWithApplications = lockRepository.getLockWithApplications(lockId)
        if (lockWithApplications == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Lock not found."
            )
            return
        }

        val lock = lockWithApplications.lock
        val remaining = LockTiming.remainingDuration(
            lock = lock,
            nowElapsedRealtime = timeProvider.elapsedRealtimeMillis(),
            nowWallTime = timeProvider.wallTimeMillis()
        )
        val appLabels = lockWithApplications.applications.map { lockApplication ->
            appRepository.getApp(lockApplication.packageId)?.displayName
                ?: lockApplication.packageId
        }

        _uiState.value = _uiState.value.copy(
            status = lock.status,
            remainingMillis = remaining.millis,
            committedEndWallTime = lock.committedEndWallTime,
            blockedApps = appLabels,
            isLoading = false,
            errorMessage = null
        )
    }

    fun setExtensionAmount(value: String) {
        _uiState.value = _uiState.value.copy(
            extensionAmount = value.filter { it.isDigit() },
            errorMessage = null
        )
    }

    fun setExtensionUnit(unit: LockDurationUnit) {
        _uiState.value = _uiState.value.copy(
            extensionUnit = unit,
            errorMessage = null
        )
    }

    fun setExtensionConfirmed(confirmed: Boolean) {
        _uiState.value = _uiState.value.copy(
            extensionConfirmed = confirmed,
            errorMessage = null
        )
    }

    fun extendLock() {
        val amount = _uiState.value.extensionAmount.toLongOrNull()
        when {
            amount == null || amount <= 0 -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Enter an extension greater than zero."
                )
                return
            }
            !_uiState.value.extensionConfirmed -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Confirm the irreversible extension before continuing."
                )
                return
            }
        }

        val duration = runCatching {
            when (_uiState.value.extensionUnit) {
                LockDurationUnit.MINUTES -> LockDuration.fromMinutes(amount)
                LockDurationUnit.HOURS -> LockDuration.fromHours(amount)
                LockDurationUnit.DAYS -> LockDuration.fromDays(amount)
                LockDurationUnit.WEEKS -> LockDuration.fromWeeks(amount)
                LockDurationUnit.CUSTOM -> LockDuration.fromMillis(amount)
            }
        }.getOrElse { throwable ->
            _uiState.value = _uiState.value.copy(
                errorMessage = throwable.message ?: "Invalid extension."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            when (val result = lockUseCases.extendLock(lockId, duration)) {
                is LockUseCaseResult.Created -> Unit
                is LockUseCaseResult.Extended -> {
                    _uiState.value = _uiState.value.copy(
                        remainingMillis = result.resultingRemainingDuration,
                        extensionAmount = "",
                        extensionConfirmed = false
                    )
                    refresh()
                }
                is LockUseCaseResult.Rejected -> {
                    _uiState.value = _uiState.value.copy(errorMessage = result.reason)
                }
            }
            _uiState.value = _uiState.value.copy(isSubmitting = false)
        }
    }

    fun rejectWeakeningAction(action: LockWeakeningAction) {
        val rejection = LockStateMachine.rejectWeakening(action)
        _uiState.value = _uiState.value.copy(
            rejectedActionMessage = rejection.reason
        )
    }
}

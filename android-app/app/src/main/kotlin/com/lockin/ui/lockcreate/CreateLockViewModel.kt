package com.lockin.ui.lockcreate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.data.entities.ApplicationEntity
import com.lockin.domain.appcatalog.AppCatalogScanner
import com.lockin.domain.lock.CreateLockRequest
import com.lockin.domain.lock.LockDuration
import com.lockin.domain.lock.LockDurationUnit
import com.lockin.domain.lock.LockUseCaseResult
import com.lockin.domain.lock.LockUseCases
import com.lockin.domain.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CreateLockUiState(
    val apps: List<ApplicationEntity> = emptyList(),
    val selectedPackageIds: Set<String> = emptySet(),
    val durationAmount: String = "",
    val durationUnit: LockDurationUnit = LockDurationUnit.MINUTES,
    val confirmationAccepted: Boolean = false,
    val isSubmitting: Boolean = false,
    val isLoadingApps: Boolean = false,
    val createdLockId: Long? = null,
    val extendedLockId: Long? = null,
    val resultingRemainingDuration: Long? = null,
    val errorMessage: String? = null
) {
    val canSubmit: Boolean =
        selectedPackageIds.isNotEmpty() &&
            durationAmount.toLongOrNull()?.let { it > 0 } == true &&
            confirmationAccepted &&
            !isSubmitting
}

class CreateLockViewModel(
    private val appRepository: AppRepository,
    private val appCatalogScanner: AppCatalogScanner? = null,
    private val lockUseCases: LockUseCases
) : ViewModel() {
    private val selectedPackageIds = MutableStateFlow<Set<String>>(emptySet())
    private val durationAmount = MutableStateFlow("")
    private val durationUnit = MutableStateFlow(LockDurationUnit.MINUTES)
    private val confirmationAccepted = MutableStateFlow(false)
    private val isSubmitting = MutableStateFlow(false)
    private val isLoadingApps = MutableStateFlow(false)
    private val createdLockId = MutableStateFlow<Long?>(null)
    private val extendedLockId = MutableStateFlow<Long?>(null)
    private val resultingRemainingDuration = MutableStateFlow<Long?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CreateLockUiState> = combine(
        appRepository.observeLockableInstalledApps(),
        selectedPackageIds,
        durationAmount,
        durationUnit,
        confirmationAccepted,
        isSubmitting,
        isLoadingApps,
        createdLockId,
        extendedLockId,
        resultingRemainingDuration,
        errorMessage
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        CreateLockUiState(
            apps = values[0] as List<ApplicationEntity>,
            selectedPackageIds = values[1] as Set<String>,
            durationAmount = values[2] as String,
            durationUnit = values[3] as LockDurationUnit,
            confirmationAccepted = values[4] as Boolean,
            isSubmitting = values[5] as Boolean,
            isLoadingApps = values[6] as Boolean,
            createdLockId = values[7] as Long?,
            extendedLockId = values[8] as Long?,
            resultingRemainingDuration = values[9] as Long?,
            errorMessage = values[10] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = CreateLockUiState()
    )

    init {
        refreshInstalledApps()
    }

    fun refreshInstalledApps() {
        val scanner = appCatalogScanner ?: return
        viewModelScope.launch {
            isLoadingApps.value = true
            runCatching {
                val now = System.currentTimeMillis()
                val applications = scanner.scanInstalledApps().map { identity ->
                    ApplicationEntity(
                        packageId = identity.packageId,
                        displayName = identity.displayName,
                        iconRef = identity.iconRef,
                        isInstalled = identity.isInstalled,
                        isLockinApp = identity.isLockinApp,
                        isPolicyExempt = identity.isPolicyExempt,
                        lastSeenAt = now
                    )
                }
                appRepository.upsertApps(applications)
            }.onFailure { throwable ->
                errorMessage.value = throwable.message ?: "Unable to load installed apps."
            }
            isLoadingApps.value = false
        }
    }

    fun toggleApp(packageId: String) {
        selectedPackageIds.value = if (packageId in selectedPackageIds.value) {
            selectedPackageIds.value - packageId
        } else {
            selectedPackageIds.value + packageId
        }
        errorMessage.value = null
    }

    fun setDurationAmount(value: String) {
        durationAmount.value = value.filter { it.isDigit() }
        errorMessage.value = null
    }

    fun setDurationUnit(unit: LockDurationUnit) {
        durationUnit.value = unit
        errorMessage.value = null
    }

    fun setConfirmationAccepted(accepted: Boolean) {
        confirmationAccepted.value = accepted
        errorMessage.value = null
    }

    fun createLock() {
        val duration = validatedDuration(
            requireSelection = true
        ) ?: return

        viewModelScope.launch {
            isSubmitting.value = true
            errorMessage.value = null
            when (
                val result = lockUseCases.createLock(
                    CreateLockRequest(
                        packageIds = selectedPackageIds.value,
                        duration = duration
                    )
                )
            ) {
                is LockUseCaseResult.Created -> createdLockId.value = result.lockId
                is LockUseCaseResult.Extended -> {
                    extendedLockId.value = result.lockId
                    resultingRemainingDuration.value = result.resultingRemainingDuration
                }
                is LockUseCaseResult.Rejected -> errorMessage.value = result.reason
            }
            isSubmitting.value = false
        }
    }

    fun extendLock(lockId: Long) {
        val duration = validatedDuration(
            requireSelection = false
        ) ?: return

        viewModelScope.launch {
            isSubmitting.value = true
            errorMessage.value = null
            when (val result = lockUseCases.extendLock(lockId, duration)) {
                is LockUseCaseResult.Created -> createdLockId.value = result.lockId
                is LockUseCaseResult.Extended -> {
                    extendedLockId.value = result.lockId
                    resultingRemainingDuration.value = result.resultingRemainingDuration
                }
                is LockUseCaseResult.Rejected -> errorMessage.value = result.reason
            }
            isSubmitting.value = false
        }
    }

    private fun validatedDuration(requireSelection: Boolean): LockDuration? {
        val amount = durationAmount.value.toLongOrNull()
        when {
            requireSelection && selectedPackageIds.value.isEmpty() -> {
                errorMessage.value = "Select at least one app to lock."
                return null
            }
            amount == null || amount <= 0 -> {
                errorMessage.value = "Enter a duration greater than zero."
                return null
            }
            !confirmationAccepted.value -> {
                errorMessage.value = "Confirm the irreversible lock before continuing."
                return null
            }
        }

        return runCatching {
            when (durationUnit.value) {
                LockDurationUnit.MINUTES -> LockDuration.fromMinutes(amount)
                LockDurationUnit.HOURS -> LockDuration.fromHours(amount)
                LockDurationUnit.DAYS -> LockDuration.fromDays(amount)
                LockDurationUnit.WEEKS -> LockDuration.fromWeeks(amount)
                LockDurationUnit.CUSTOM -> LockDuration.fromMillis(amount)
            }
        }.getOrElse { throwable ->
            errorMessage.value = throwable.message ?: "Invalid duration."
            null
        }
    }
}

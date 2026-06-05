package com.lockin.ui.home

import androidx.lifecycle.ViewModel
import com.lockin.data.dao.LockWithApplications
import com.lockin.data.entities.ApplicationEntity
import com.lockin.domain.lock.LockTiming
import com.lockin.domain.lock.TimeProvider
import com.lockin.domain.repository.AppRepository
import com.lockin.domain.repository.LockRepository
import com.lockin.domain.statistics.StatisticsCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

data class HomeActiveLockItem(
    val id: Long,
    val title: String,
    val remainingLabel: String,
    val blockedApps: List<String>
)

data class HomeUiState(
    val activeLocks: List<HomeActiveLockItem> = emptyList()
) {
    val hasActiveLocks: Boolean = activeLocks.isNotEmpty()
}

class HomeViewModel(
    lockRepository: LockRepository,
    appRepository: AppRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        lockRepository.observeActiveLocks(),
        appRepository.observeAllApps()
    ) { activeLocks, apps ->
        HomeUiState(
            activeLocks = activeLocks.toHomeItems(apps)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = HomeUiState()
    )

    private fun List<LockWithApplications>.toHomeItems(
        apps: List<ApplicationEntity>
    ): List<HomeActiveLockItem> {
        val namesByPackage = apps.associate { it.packageId to it.displayName }
        val nowElapsed = timeProvider.elapsedRealtimeMillis()
        return sortedBy { it.lock.committedEndWallTime }.map { lockWithApplications ->
            val remaining = LockTiming.remainingDuration(lockWithApplications.lock, nowElapsed)
            HomeActiveLockItem(
                id = lockWithApplications.lock.id,
                title = "Lock #${lockWithApplications.lock.id}",
                remainingLabel = StatisticsCalculator.durationLabel(remaining.millis),
                blockedApps = lockWithApplications.applications
                    .map { namesByPackage[it.packageId] ?: it.packageId }
                    .sorted()
            )
        }
    }
}

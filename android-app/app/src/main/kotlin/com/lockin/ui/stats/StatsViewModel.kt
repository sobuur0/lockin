package com.lockin.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.domain.repository.StatisticsRepository
import com.lockin.domain.statistics.StatisticSummary
import com.lockin.domain.statistics.StatisticsCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StatsUiState(
    val summary: StatisticSummary = StatisticsCalculator.calculate(
        totals = com.lockin.data.dao.StatisticsTotals(
            totalLockDuration = null,
            completedLockSessionCount = 0,
            longestLockDuration = null,
            averageLockDuration = null
        ),
        mostBlockedApplications = emptyList(),
        uniqueBlockedApplicationCount = 0,
        mostFrequentlyUsedMood = null
    )
)

class StatsViewModel(
    statisticsRepository: StatisticsRepository
) : ViewModel() {
    val uiState: StateFlow<StatsUiState> = combine(
        statisticsRepository.observeTotals(),
        statisticsRepository.observeMostBlockedApplications(),
        statisticsRepository.observeUniqueBlockedApplicationCount(),
        statisticsRepository.observeMostFrequentlyUsedMood()
    ) { totals, mostBlockedApplications, uniqueBlockedApplicationCount, mostFrequentlyUsedMood ->
        StatsUiState(
            summary = StatisticsCalculator.calculate(
                totals = totals,
                mostBlockedApplications = mostBlockedApplications,
                uniqueBlockedApplicationCount = uniqueBlockedApplicationCount,
                mostFrequentlyUsedMood = mostFrequentlyUsedMood
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = StatsUiState()
    )
}

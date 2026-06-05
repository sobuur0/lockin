package com.lockin.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lockin.domain.statistics.BlockedAppSummary
import com.lockin.ui.app.LockinEmptyState
import com.lockin.ui.app.LockinSection

@Composable
fun StatsScreen(
    state: StatsUiState,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier.fillMaxSize()) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (state.summary.isEmpty) {
                item {
                    LockinEmptyState(
                        title = "No completed locks",
                        body = "Completed lock history will appear here."
                    )
                }
            } else {
                item {
                    LockinSection(title = "Totals") {
                        StatRow("Total duration", state.summary.totalDurationLabel)
                        StatRow("Completed locks", state.summary.completedLockSessionCount.toString())
                        StatRow("Unique apps", state.summary.uniqueBlockedApplicationCount.toString())
                        StatRow("Longest lock", state.summary.longestDurationLabel)
                        StatRow("Average lock", state.summary.averageDurationLabel)
                        StatRow(
                            label = "Most used mood",
                            value = state.summary.mostFrequentlyUsedMoodName ?: "None"
                        )
                    }
                }
                item {
                    Text(
                        text = "Most Blocked Apps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (state.summary.mostBlockedApplications.isEmpty()) {
                    item {
                        LockinEmptyState(
                            title = "No app history",
                            body = "Completed lock app history will appear here."
                        )
                    }
                } else {
                    items(
                        items = state.summary.mostBlockedApplications,
                        key = { it.packageId }
                    ) { app ->
                        BlockedAppRow(app)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BlockedAppRow(app: BlockedAppSummary) {
    LockinSection(title = app.displayName) {
        StatRow("Package", app.packageId)
        StatRow("Completed locks", app.blockCount.toString())
    }
}

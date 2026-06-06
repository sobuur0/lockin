package com.lockin.ui.lockdetail

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lockin.data.entities.LockStatus
import com.lockin.domain.lock.LockDurationUnit
import com.lockin.ui.app.ActiveLockCard
import com.lockin.ui.app.IrreversibleConfirmationCopy
import com.lockin.ui.app.LockinDurationInput
import com.lockin.ui.app.LockinEmptyState
import com.lockin.ui.app.LockinSection
import kotlinx.coroutines.delay

@Composable
fun LockDetailScreen(
    state: LockDetailUiState,
    onExtensionAmountChange: (String) -> Unit,
    onExtensionUnitChange: (LockDurationUnit) -> Unit,
    onExtensionConfirmationChange: (Boolean) -> Unit,
    onExtendLock: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(state.lockId, state.status) {
        while (state.status != LockStatus.COMPLETED) {
            delay(1_000L)
            onRefresh()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Lock Detail",
                style = MaterialTheme.typography.headlineSmall
            )

            when {
                state.isLoading -> {
                    LockinEmptyState(
                        title = "Loading lock",
                        body = "Reading local lock state."
                    )
                }
                state.errorMessage == "Lock not found." -> {
                    LockinEmptyState(
                        title = "Lock not found",
                        body = "The requested lock is unavailable."
                    )
                }
                else -> {
                    ActiveLockCard(
                        title = state.status?.name ?: "Lock",
                        remainingTime = state.remainingLabel,
                        blockedApps = state.blockedApps
                    )

                    IrreversibleConfirmationCopy()

                    LockinSection(title = "Extension") {
                        LockinDurationInput(
                            value = state.extensionAmount,
                            label = if (state.extensionUnit == LockDurationUnit.CUSTOM) {
                                "Extension in milliseconds"
                            } else {
                                "Extend by"
                            },
                            onValueChange = onExtensionAmountChange,
                            supportingText = if (state.extensionUnit == LockDurationUnit.CUSTOM) {
                                "Custom values are interpreted as milliseconds."
                            } else {
                                "Extension is added to the remaining duration."
                            }
                        )

                        ExtensionUnitRow(
                            selected = state.extensionUnit,
                            onSelected = onExtensionUnitChange
                        )
                    }

                    state.errorMessage?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    state.rejectedActionMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    LockinSection(title = "Confirmation") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = state.extensionConfirmed,
                                onCheckedChange = onExtensionConfirmationChange
                            )
                            Text(
                                text = "I understand this extension cannot be reversed.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Button(
                            onClick = onExtendLock,
                            enabled = state.canExtend,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (state.isSubmitting) "Extending" else "Extend Lock")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtensionUnitRow(
    selected: LockDurationUnit,
    onSelected: (LockDurationUnit) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            LockDurationUnit.MINUTES to "Min",
            LockDurationUnit.HOURS to "Hours",
            LockDurationUnit.DAYS to "Days",
            LockDurationUnit.WEEKS to "Weeks",
            LockDurationUnit.CUSTOM to "Custom"
        ).forEach { (unit, label) ->
            FilterChip(
                selected = selected == unit,
                onClick = { onSelected(unit) },
                label = { Text(label) }
            )
        }
    }
}

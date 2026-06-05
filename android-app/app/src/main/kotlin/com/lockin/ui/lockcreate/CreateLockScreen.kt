package com.lockin.ui.lockcreate

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.lockin.domain.lock.LockDurationUnit
import com.lockin.ui.app.IrreversibleConfirmationCopy
import com.lockin.ui.app.LockinAppRow
import com.lockin.ui.app.LockinDurationInput
import com.lockin.ui.app.LockinEmptyState
import com.lockin.ui.app.LockinSection

@Composable
fun CreateLockScreen(
    state: CreateLockUiState,
    onToggleApp: (String) -> Unit,
    onDurationAmountChange: (String) -> Unit,
    onDurationUnitChange: (LockDurationUnit) -> Unit,
    onConfirmationChange: (Boolean) -> Unit,
    onCreateLock: () -> Unit,
    onLockCreated: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(state.createdLockId) {
        state.createdLockId?.let(onLockCreated)
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
                text = "Create Lock",
                style = MaterialTheme.typography.headlineSmall
            )

            LockinSection(title = "Duration") {
                LockinDurationInput(
                    value = state.durationAmount,
                    label = if (state.durationUnit == LockDurationUnit.CUSTOM) {
                        "Duration in milliseconds"
                    } else {
                        "Duration"
                    },
                    onValueChange = onDurationAmountChange,
                    supportingText = if (state.durationUnit == LockDurationUnit.CUSTOM) {
                        "Custom values are interpreted as milliseconds."
                    } else {
                        "Choose a whole number and unit."
                    }
                )

                DurationUnitRow(
                    selected = state.durationUnit,
                    onSelected = onDurationUnitChange
                )
            }

            IrreversibleConfirmationCopy()

            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            LockinSection(title = "Confirmation") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.confirmationAccepted,
                        onCheckedChange = onConfirmationChange
                    )
                    Text(
                        text = "I understand this lock cannot be undone in Lockin.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = onCreateLock,
                    enabled = state.canSubmit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isSubmitting) "Creating" else "Start Lock")
                }
            }

            Text(
                text = "Apps",
                style = MaterialTheme.typography.titleMedium
            )

            when {
                state.isLoadingApps -> {
                    LockinEmptyState(
                        title = "Loading apps",
                        body = "Reading installed applications on this device."
                    )
                }
                state.apps.isEmpty() -> {
                    LockinEmptyState(
                        title = "No lockable apps",
                        body = "No installed non-exempt apps are available yet."
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = state.apps,
                            key = { it.packageId }
                        ) { app ->
                            LockinAppRow(
                                displayName = app.displayName,
                                packageId = app.packageId,
                                selected = app.packageId in state.selectedPackageIds,
                                enabled = true,
                                onSelectedChange = { onToggleApp(app.packageId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DurationUnitRow(
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

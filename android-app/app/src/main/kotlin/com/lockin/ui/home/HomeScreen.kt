package com.lockin.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lockin.ui.app.ActiveLockCard
import com.lockin.ui.app.LockinEmptyState
import com.lockin.ui.app.LockinSection

@Composable
fun HomeScreen(
    state: HomeUiState,
    onCreateLock: () -> Unit,
    onGroups: () -> Unit,
    onMoods: () -> Unit,
    onStats: () -> Unit,
    onLockSelected: (Long) -> Unit,
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
                    text = "Lockin",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                LockinSection(title = "Actions") {
                    Button(
                        onClick = onCreateLock,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Lock")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onGroups,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Groups")
                        }
                        OutlinedButton(
                            onClick = onMoods,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Moods")
                        }
                    }
                    OutlinedButton(
                        onClick = onStats,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Statistics")
                    }
                }
            }
            item {
                Text(
                    text = "Active Locks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (state.activeLocks.isEmpty()) {
                item {
                    LockinEmptyState(
                        title = "No active locks",
                        body = "Create a lock to block selected apps."
                    )
                }
            } else {
                items(
                    items = state.activeLocks,
                    key = { it.id }
                ) { lock ->
                    ActiveLockCard(
                        title = lock.title,
                        remainingTime = lock.remainingLabel,
                        blockedApps = lock.blockedApps,
                        modifier = Modifier.clickable { onLockSelected(lock.id) }
                    )
                }
            }
        }
    }
}

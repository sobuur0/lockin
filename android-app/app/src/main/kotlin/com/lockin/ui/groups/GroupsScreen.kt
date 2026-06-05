package com.lockin.ui.groups

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.data.dao.LockGroupWithApplications
import com.lockin.data.entities.ApplicationEntity
import com.lockin.domain.lock.LockDuration
import com.lockin.domain.lock.LockDurationUnit
import com.lockin.domain.repository.AppRepository
import com.lockin.domain.repository.TemplateRepository
import com.lockin.domain.templates.SaveLockGroupRequest
import com.lockin.domain.templates.StartLockFromGroupRequest
import com.lockin.domain.templates.TemplateUseCaseResult
import com.lockin.domain.templates.TemplateUseCases
import com.lockin.ui.app.LockinAppRow
import com.lockin.ui.app.LockinEmptyState
import com.lockin.ui.app.LockinSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GroupListItem(
    val id: Long,
    val name: String,
    val packageIds: Set<String>,
    val appSummary: String
)

data class GroupsUiState(
    val apps: List<ApplicationEntity> = emptyList(),
    val groups: List<GroupListItem> = emptyList(),
    val editingGroupId: Long? = null,
    val name: String = "",
    val selectedPackageIds: Set<String> = emptySet(),
    val startDurationAmount: String = "",
    val startDurationUnit: LockDurationUnit = LockDurationUnit.MINUTES,
    val startConfirmationAccepted: Boolean = false,
    val isSaving: Boolean = false,
    val isStarting: Boolean = false,
    val startedLockId: Long? = null,
    val errorMessage: String? = null
) {
    val canSave: Boolean = name.isNotBlank() && selectedPackageIds.isNotEmpty() && !isSaving
    val canStart: Boolean =
        startDurationAmount.toLongOrNull()?.let { it > 0 } == true &&
            startConfirmationAccepted &&
            !isStarting
}

class GroupsViewModel(
    private val appRepository: AppRepository,
    private val templateRepository: TemplateRepository,
    private val templateUseCases: TemplateUseCases
) : ViewModel() {
    private val editingGroupId = MutableStateFlow<Long?>(null)
    private val name = MutableStateFlow("")
    private val selectedPackageIds = MutableStateFlow<Set<String>>(emptySet())
    private val startDurationAmount = MutableStateFlow("")
    private val startDurationUnit = MutableStateFlow(LockDurationUnit.MINUTES)
    private val startConfirmationAccepted = MutableStateFlow(false)
    private val isSaving = MutableStateFlow(false)
    private val isStarting = MutableStateFlow(false)
    private val startedLockId = MutableStateFlow<Long?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GroupsUiState> = combine(
        appRepository.observeLockableInstalledApps(),
        templateRepository.observeGroups(),
        editingGroupId,
        name,
        selectedPackageIds,
        startDurationAmount,
        startDurationUnit,
        startConfirmationAccepted,
        isSaving,
        isStarting,
        startedLockId,
        errorMessage
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val apps = values[0] as List<ApplicationEntity>
        @Suppress("UNCHECKED_CAST")
        val groups = values[1] as List<LockGroupWithApplications>
        val appNames = apps.associate { it.packageId to it.displayName }
        GroupsUiState(
            apps = apps,
            groups = groups.map { group ->
                val packageIds = group.applications.map { it.packageId }.toSet()
                GroupListItem(
                    id = group.group.id,
                    name = group.group.name,
                    packageIds = packageIds,
                    appSummary = packageIds
                        .map { appNames[it] ?: it }
                        .sorted()
                        .joinToString()
                )
            },
            editingGroupId = values[2] as Long?,
            name = values[3] as String,
            selectedPackageIds = values[4] as Set<String>,
            startDurationAmount = values[5] as String,
            startDurationUnit = values[6] as LockDurationUnit,
            startConfirmationAccepted = values[7] as Boolean,
            isSaving = values[8] as Boolean,
            isStarting = values[9] as Boolean,
            startedLockId = values[10] as Long?,
            errorMessage = values[11] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = GroupsUiState()
    )

    fun setName(value: String) {
        name.value = value
        errorMessage.value = null
    }

    fun toggleApp(packageId: String) {
        selectedPackageIds.value = if (packageId in selectedPackageIds.value) {
            selectedPackageIds.value - packageId
        } else {
            selectedPackageIds.value + packageId
        }
        errorMessage.value = null
    }

    fun editGroup(groupId: Long) {
        val group = uiState.value.groups.firstOrNull { it.id == groupId } ?: return
        editingGroupId.value = group.id
        name.value = group.name
        selectedPackageIds.value = group.packageIds
        errorMessage.value = null
    }

    fun clearEditor() {
        editingGroupId.value = null
        name.value = ""
        selectedPackageIds.value = emptySet()
        errorMessage.value = null
    }

    fun setStartDurationAmount(value: String) {
        startDurationAmount.value = value.filter { it.isDigit() }
        errorMessage.value = null
    }

    fun setStartDurationUnit(unit: LockDurationUnit) {
        startDurationUnit.value = unit
        errorMessage.value = null
    }

    fun setStartConfirmationAccepted(accepted: Boolean) {
        startConfirmationAccepted.value = accepted
        errorMessage.value = null
    }

    fun saveGroup() {
        viewModelScope.launch {
            isSaving.value = true
            when (
                val result = templateUseCases.saveGroup(
                    SaveLockGroupRequest(
                        id = editingGroupId.value,
                        name = name.value,
                        packageIds = selectedPackageIds.value
                    )
                )
            ) {
                is TemplateUseCaseResult.Saved -> clearEditor()
                is TemplateUseCaseResult.Archived -> Unit
                is TemplateUseCaseResult.Started -> startedLockId.value = result.lockId
                is TemplateUseCaseResult.Rejected -> errorMessage.value = result.reason
            }
            isSaving.value = false
        }
    }

    fun archiveGroup(groupId: Long) {
        viewModelScope.launch {
            when (val result = templateUseCases.archiveGroup(groupId)) {
                is TemplateUseCaseResult.Rejected -> errorMessage.value = result.reason
                else -> if (editingGroupId.value == groupId) clearEditor()
            }
        }
    }

    fun startGroup(groupId: Long) {
        val duration = validatedStartDuration() ?: return
        viewModelScope.launch {
            isStarting.value = true
            when (
                val result = templateUseCases.startLockFromGroup(
                    StartLockFromGroupRequest(
                        groupId = groupId,
                        duration = duration
                    )
                )
            ) {
                is TemplateUseCaseResult.Started -> startedLockId.value = result.lockId
                is TemplateUseCaseResult.Rejected -> errorMessage.value = result.reason
                is TemplateUseCaseResult.Saved,
                is TemplateUseCaseResult.Archived -> Unit
            }
            isStarting.value = false
        }
    }

    private fun validatedStartDuration(): LockDuration? {
        val amount = startDurationAmount.value.toLongOrNull()
        if (amount == null || amount <= 0) {
            errorMessage.value = "Enter a duration greater than zero."
            return null
        }
        if (!startConfirmationAccepted.value) {
            errorMessage.value = "Confirm the irreversible lock before continuing."
            return null
        }
        return durationFrom(amount, startDurationUnit.value)
    }
}

@Composable
fun GroupsScreen(
    state: GroupsUiState,
    onNameChange: (String) -> Unit,
    onToggleApp: (String) -> Unit,
    onSaveGroup: () -> Unit,
    onEditGroup: (Long) -> Unit,
    onArchiveGroup: (Long) -> Unit,
    onClearEditor: () -> Unit,
    onStartDurationAmountChange: (String) -> Unit,
    onStartDurationUnitChange: (LockDurationUnit) -> Unit,
    onStartConfirmationChange: (Boolean) -> Unit,
    onStartGroup: (Long) -> Unit,
    onLockStarted: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(state.startedLockId) {
        state.startedLockId?.let(onLockStarted)
    }

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
                    text = "Groups",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                LockinSection(
                    title = if (state.editingGroupId == null) "New Group" else "Edit Group"
                ) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = onNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Name") },
                        singleLine = true
                    )
                    state.apps.forEach { app ->
                        LockinAppRow(
                            displayName = app.displayName,
                            packageId = app.packageId,
                            selected = app.packageId in state.selectedPackageIds,
                            enabled = true,
                            onSelectedChange = { onToggleApp(app.packageId) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onSaveGroup,
                            enabled = state.canSave
                        ) {
                            Text(if (state.editingGroupId == null) "Save Group" else "Update Group")
                        }
                        if (state.editingGroupId != null) {
                            OutlinedButton(onClick = onClearEditor) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
            item {
                LockinSection(title = "Start From Group") {
                    OutlinedTextField(
                        value = state.startDurationAmount,
                        onValueChange = onStartDurationAmountChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Duration") },
                        singleLine = true
                    )
                    DurationUnitRow(
                        selected = state.startDurationUnit,
                        onSelected = onStartDurationUnitChange
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.startConfirmationAccepted,
                            onCheckedChange = onStartConfirmationChange
                        )
                        Text(
                            text = "I understand this lock cannot be undone in Lockin.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            state.errorMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (state.groups.isEmpty()) {
                item {
                    LockinEmptyState(
                        title = "No groups",
                        body = "Saved groups will appear here."
                    )
                }
            } else {
                items(
                    items = state.groups,
                    key = { it.id }
                ) { group ->
                    GroupRow(
                        group = group,
                        canStart = state.canStart,
                        onEdit = { onEditGroup(group.id) },
                        onArchive = { onArchiveGroup(group.id) },
                        onStart = { onStartGroup(group.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupRow(
    group: GroupListItem,
    canStart: Boolean,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onStart: () -> Unit
) {
    LockinSection(title = group.name) {
        Text(
            text = group.appSummary.ifBlank { "No apps" },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onStart,
                enabled = canStart
            ) {
                Text("Start")
            }
            OutlinedButton(onClick = onEdit) {
                Text("Edit")
            }
            OutlinedButton(onClick = onArchive) {
                Text("Archive")
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

private fun durationFrom(amount: Long, unit: LockDurationUnit): LockDuration? =
    runCatching {
        when (unit) {
            LockDurationUnit.MINUTES -> LockDuration.fromMinutes(amount)
            LockDurationUnit.HOURS -> LockDuration.fromHours(amount)
            LockDurationUnit.DAYS -> LockDuration.fromDays(amount)
            LockDurationUnit.WEEKS -> LockDuration.fromWeeks(amount)
            LockDurationUnit.CUSTOM -> LockDuration.fromMillis(amount)
        }
    }.getOrNull()

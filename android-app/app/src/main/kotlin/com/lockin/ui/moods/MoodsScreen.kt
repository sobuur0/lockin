package com.lockin.ui.moods

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
import com.lockin.data.dao.MoodWithApplications
import com.lockin.data.entities.ApplicationEntity
import com.lockin.domain.lock.LockDuration
import com.lockin.domain.lock.LockDurationUnit
import com.lockin.domain.repository.AppRepository
import com.lockin.domain.repository.TemplateRepository
import com.lockin.domain.templates.SaveMoodRequest
import com.lockin.domain.templates.StartLockFromMoodRequest
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

data class MoodListItem(
    val id: Long,
    val name: String,
    val packageIds: Set<String>,
    val appSummary: String,
    val defaultDurationMillis: Long?,
    val defaultDurationLabel: String
)

data class MoodsUiState(
    val apps: List<ApplicationEntity> = emptyList(),
    val moods: List<MoodListItem> = emptyList(),
    val editingMoodId: Long? = null,
    val name: String = "",
    val selectedPackageIds: Set<String> = emptySet(),
    val defaultDurationAmount: String = "",
    val defaultDurationUnit: LockDurationUnit = LockDurationUnit.MINUTES,
    val startDurationAmount: String = "",
    val startDurationUnit: LockDurationUnit = LockDurationUnit.MINUTES,
    val startConfirmationAccepted: Boolean = false,
    val isSaving: Boolean = false,
    val isStarting: Boolean = false,
    val startedLockId: Long? = null,
    val errorMessage: String? = null
) {
    val canSave: Boolean = name.isNotBlank() && selectedPackageIds.isNotEmpty() && !isSaving
    val canStart: Boolean = startConfirmationAccepted && !isStarting
}

class MoodsViewModel(
    private val appRepository: AppRepository,
    private val templateRepository: TemplateRepository,
    private val templateUseCases: TemplateUseCases
) : ViewModel() {
    private val editingMoodId = MutableStateFlow<Long?>(null)
    private val name = MutableStateFlow("")
    private val selectedPackageIds = MutableStateFlow<Set<String>>(emptySet())
    private val defaultDurationAmount = MutableStateFlow("")
    private val defaultDurationUnit = MutableStateFlow(LockDurationUnit.MINUTES)
    private val startDurationAmount = MutableStateFlow("")
    private val startDurationUnit = MutableStateFlow(LockDurationUnit.MINUTES)
    private val startConfirmationAccepted = MutableStateFlow(false)
    private val isSaving = MutableStateFlow(false)
    private val isStarting = MutableStateFlow(false)
    private val startedLockId = MutableStateFlow<Long?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MoodsUiState> = combine(
        appRepository.observeLockableInstalledApps(),
        templateRepository.observeMoods(),
        editingMoodId,
        name,
        selectedPackageIds,
        defaultDurationAmount,
        defaultDurationUnit,
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
        val moods = values[1] as List<MoodWithApplications>
        val appNames = apps.associate { it.packageId to it.displayName }
        MoodsUiState(
            apps = apps,
            moods = moods.map { mood ->
                val packageIds = mood.applications.map { it.packageId }.toSet()
                MoodListItem(
                    id = mood.mood.id,
                    name = mood.mood.name,
                    packageIds = packageIds,
                    appSummary = packageIds
                        .map { appNames[it] ?: it }
                        .sorted()
                        .joinToString(),
                    defaultDurationMillis = mood.mood.defaultDuration,
                    defaultDurationLabel = durationLabel(mood.mood.defaultDuration)
                )
            },
            editingMoodId = values[2] as Long?,
            name = values[3] as String,
            selectedPackageIds = values[4] as Set<String>,
            defaultDurationAmount = values[5] as String,
            defaultDurationUnit = values[6] as LockDurationUnit,
            startDurationAmount = values[7] as String,
            startDurationUnit = values[8] as LockDurationUnit,
            startConfirmationAccepted = values[9] as Boolean,
            isSaving = values[10] as Boolean,
            isStarting = values[11] as Boolean,
            startedLockId = values[12] as Long?,
            errorMessage = values[13] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MoodsUiState()
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

    fun editMood(moodId: Long) {
        val mood = uiState.value.moods.firstOrNull { it.id == moodId } ?: return
        editingMoodId.value = mood.id
        name.value = mood.name
        selectedPackageIds.value = mood.packageIds
        val defaultMillis = mood.defaultDurationMillis
        defaultDurationAmount.value = defaultMillis?.toString().orEmpty()
        defaultDurationUnit.value = LockDurationUnit.CUSTOM
        errorMessage.value = null
    }

    fun clearEditor() {
        editingMoodId.value = null
        name.value = ""
        selectedPackageIds.value = emptySet()
        defaultDurationAmount.value = ""
        defaultDurationUnit.value = LockDurationUnit.MINUTES
        errorMessage.value = null
    }

    fun setDefaultDurationAmount(value: String) {
        defaultDurationAmount.value = value.filter { it.isDigit() }
        errorMessage.value = null
    }

    fun setDefaultDurationUnit(unit: LockDurationUnit) {
        defaultDurationUnit.value = unit
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

    fun saveMood() {
        val duration = if (defaultDurationAmount.value.isBlank()) {
            null
        } else {
            optionalDuration(defaultDurationAmount.value, defaultDurationUnit.value) ?: return
        }
        viewModelScope.launch {
            isSaving.value = true
            when (
                val result = templateUseCases.saveMood(
                    SaveMoodRequest(
                        id = editingMoodId.value,
                        name = name.value,
                        packageIds = selectedPackageIds.value,
                        defaultDuration = duration
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

    fun archiveMood(moodId: Long) {
        viewModelScope.launch {
            when (val result = templateUseCases.archiveMood(moodId)) {
                is TemplateUseCaseResult.Rejected -> errorMessage.value = result.reason
                else -> if (editingMoodId.value == moodId) clearEditor()
            }
        }
    }

    fun startMood(moodId: Long) {
        if (!startConfirmationAccepted.value) {
            errorMessage.value = "Confirm the irreversible lock before continuing."
            return
        }
        val override = if (startDurationAmount.value.isBlank()) {
            null
        } else {
            optionalDuration(startDurationAmount.value, startDurationUnit.value) ?: return
        }
        viewModelScope.launch {
            isStarting.value = true
            when (
                val result = templateUseCases.startLockFromMood(
                    StartLockFromMoodRequest(
                        moodId = moodId,
                        durationOverride = override
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

    private fun optionalDuration(value: String, unit: LockDurationUnit): LockDuration? {
        if (value.isBlank()) {
            return null
        }
        val amount = value.toLongOrNull()
        if (amount == null || amount <= 0) {
            errorMessage.value = "Enter a duration greater than zero."
            return null
        }
        return durationFrom(amount, unit)
    }
}

@Composable
fun MoodsScreen(
    state: MoodsUiState,
    onNameChange: (String) -> Unit,
    onToggleApp: (String) -> Unit,
    onDefaultDurationAmountChange: (String) -> Unit,
    onDefaultDurationUnitChange: (LockDurationUnit) -> Unit,
    onSaveMood: () -> Unit,
    onEditMood: (Long) -> Unit,
    onArchiveMood: (Long) -> Unit,
    onClearEditor: () -> Unit,
    onStartDurationAmountChange: (String) -> Unit,
    onStartDurationUnitChange: (LockDurationUnit) -> Unit,
    onStartConfirmationChange: (Boolean) -> Unit,
    onStartMood: (Long) -> Unit,
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
                    text = "Moods",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                LockinSection(
                    title = if (state.editingMoodId == null) "New Mood" else "Edit Mood"
                ) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = onNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = state.defaultDurationAmount,
                        onValueChange = onDefaultDurationAmountChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Default duration") },
                        singleLine = true
                    )
                    DurationUnitRow(
                        selected = state.defaultDurationUnit,
                        onSelected = onDefaultDurationUnitChange
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
                            onClick = onSaveMood,
                            enabled = state.canSave
                        ) {
                            Text(if (state.editingMoodId == null) "Save Mood" else "Update Mood")
                        }
                        if (state.editingMoodId != null) {
                            OutlinedButton(onClick = onClearEditor) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
            item {
                LockinSection(title = "Start From Mood") {
                    OutlinedTextField(
                        value = state.startDurationAmount,
                        onValueChange = onStartDurationAmountChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Custom duration") },
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
            if (state.moods.isEmpty()) {
                item {
                    LockinEmptyState(
                        title = "No moods",
                        body = "Reusable moods will appear here."
                    )
                }
            } else {
                items(
                    items = state.moods,
                    key = { it.id }
                ) { mood ->
                    MoodRow(
                        mood = mood,
                        canStart = state.canStart,
                        onEdit = { onEditMood(mood.id) },
                        onArchive = { onArchiveMood(mood.id) },
                        onStart = { onStartMood(mood.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodRow(
    mood: MoodListItem,
    canStart: Boolean,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onStart: () -> Unit
) {
    LockinSection(title = mood.name) {
        Text(
            text = mood.appSummary.ifBlank { "No apps" },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = mood.defaultDurationLabel,
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

private fun durationLabel(durationMillis: Long?): String {
    if (durationMillis == null) return "No default duration"
    val minute = LockDuration.fromMinutes(1).millis
    val hour = LockDuration.fromHours(1).millis
    val day = LockDuration.fromDays(1).millis
    val week = LockDuration.fromWeeks(1).millis
    return when {
        durationMillis % week == 0L -> "${durationMillis / week} weeks"
        durationMillis % day == 0L -> "${durationMillis / day} days"
        durationMillis % hour == 0L -> "${durationMillis / hour} hours"
        durationMillis % minute == 0L -> "${durationMillis / minute} min"
        else -> "$durationMillis ms"
    }
}

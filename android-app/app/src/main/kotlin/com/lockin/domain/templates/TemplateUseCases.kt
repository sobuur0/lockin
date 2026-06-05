package com.lockin.domain.templates

import com.lockin.data.entities.LockGroupApplicationEntity
import com.lockin.data.entities.LockGroupEntity
import com.lockin.data.entities.LockSourceType
import com.lockin.data.entities.MoodApplicationEntity
import com.lockin.data.entities.MoodEntity
import com.lockin.domain.appcatalog.AppEligibilityRules
import com.lockin.domain.groups.LockGroup
import com.lockin.domain.lock.CreateLockRequest
import com.lockin.domain.lock.LockDuration
import com.lockin.domain.lock.LockUseCaseResult
import com.lockin.domain.lock.LockUseCases
import com.lockin.domain.lock.TimeProvider
import com.lockin.domain.moods.Mood
import com.lockin.domain.repository.AppRepository
import com.lockin.domain.repository.TemplateRepository

sealed interface TemplateUseCaseResult {
    data class Saved(val templateId: Long) : TemplateUseCaseResult
    data class Archived(val templateId: Long) : TemplateUseCaseResult
    data class Started(val lockId: Long) : TemplateUseCaseResult
    data class Rejected(val reason: String) : TemplateUseCaseResult
}

data class SaveLockGroupRequest(
    val id: Long? = null,
    val name: String,
    val packageIds: Set<String>
)

data class SaveMoodRequest(
    val id: Long? = null,
    val name: String,
    val packageIds: Set<String>,
    val defaultDuration: LockDuration? = null
)

data class StartLockFromGroupRequest(
    val groupId: Long,
    val duration: LockDuration
)

data class StartLockFromMoodRequest(
    val moodId: Long,
    val durationOverride: LockDuration? = null
)

class TemplateUseCases(
    private val appRepository: AppRepository,
    private val templateRepository: TemplateRepository,
    private val lockUseCases: LockUseCases,
    private val timeProvider: TimeProvider
) {
    suspend fun saveGroup(request: SaveLockGroupRequest): TemplateUseCaseResult {
        LockGroup.validateName(request.name)?.let { return TemplateUseCaseResult.Rejected(it) }
        LockGroup.validatePackageIds(request.packageIds)?.let { return TemplateUseCaseResult.Rejected(it) }
        validateApps(request.packageIds)?.let { return TemplateUseCaseResult.Rejected(it) }

        val now = timeProvider.wallTimeMillis()
        val existing = request.id?.let { templateRepository.getGroup(it)?.group }
        val group = LockGroupEntity(
            id = existing?.id ?: 0,
            name = request.name.trim(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            isArchived = false
        )
        val applications = request.packageIds.distinctSorted().map { packageId ->
            LockGroupApplicationEntity(
                groupId = group.id,
                packageId = packageId,
                addedAt = now
            )
        }
        return TemplateUseCaseResult.Saved(
            templateRepository.saveGroup(group, applications)
        )
    }

    suspend fun archiveGroup(groupId: Long): TemplateUseCaseResult {
        val existing = templateRepository.getGroup(groupId)
            ?: return TemplateUseCaseResult.Rejected("Group not found.")
        templateRepository.saveGroup(
            group = existing.group.copy(
                isArchived = true,
                updatedAt = timeProvider.wallTimeMillis()
            ),
            applications = existing.applications
        )
        return TemplateUseCaseResult.Archived(groupId)
    }

    suspend fun saveMood(request: SaveMoodRequest): TemplateUseCaseResult {
        Mood.validateName(request.name)?.let { return TemplateUseCaseResult.Rejected(it) }
        Mood.validatePackageIds(request.packageIds)?.let { return TemplateUseCaseResult.Rejected(it) }
        validateApps(request.packageIds)?.let { return TemplateUseCaseResult.Rejected(it) }

        val now = timeProvider.wallTimeMillis()
        val existing = request.id?.let { templateRepository.getMood(it)?.mood }
        val mood = MoodEntity(
            id = existing?.id ?: 0,
            name = request.name.trim(),
            defaultDuration = request.defaultDuration?.millis,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            isArchived = false
        )
        val applications = request.packageIds.distinctSorted().map { packageId ->
            MoodApplicationEntity(
                moodId = mood.id,
                packageId = packageId,
                addedAt = now
            )
        }
        return TemplateUseCaseResult.Saved(
            templateRepository.saveMood(mood, applications)
        )
    }

    suspend fun archiveMood(moodId: Long): TemplateUseCaseResult {
        val existing = templateRepository.getMood(moodId)
            ?: return TemplateUseCaseResult.Rejected("Mood not found.")
        templateRepository.saveMood(
            mood = existing.mood.copy(
                isArchived = true,
                updatedAt = timeProvider.wallTimeMillis()
            ),
            applications = existing.applications
        )
        return TemplateUseCaseResult.Archived(moodId)
    }

    suspend fun startLockFromGroup(request: StartLockFromGroupRequest): TemplateUseCaseResult {
        val group = templateRepository.getGroup(request.groupId)
            ?: return TemplateUseCaseResult.Rejected("Group not found.")
        val packageIds = group.applications.map { it.packageId }.toSet()
        if (packageIds.isEmpty()) {
            return TemplateUseCaseResult.Rejected("Select at least one app for this group.")
        }
        return lockUseCases.createLock(
            CreateLockRequest(
                packageIds = packageIds,
                duration = request.duration,
                sourceType = LockSourceType.GROUP,
                sourceId = group.group.id
            )
        ).toTemplateResult()
    }

    suspend fun startLockFromMood(request: StartLockFromMoodRequest): TemplateUseCaseResult {
        val mood = templateRepository.getMood(request.moodId)
            ?: return TemplateUseCaseResult.Rejected("Mood not found.")
        val packageIds = mood.applications.map { it.packageId }.toSet()
        if (packageIds.isEmpty()) {
            return TemplateUseCaseResult.Rejected("Select at least one app for this mood.")
        }
        val duration = request.durationOverride
            ?: mood.mood.defaultDuration?.let { LockDuration.fromMillis(it) }
            ?: return TemplateUseCaseResult.Rejected("Choose a duration before starting this mood.")

        return lockUseCases.createLock(
            CreateLockRequest(
                packageIds = packageIds,
                duration = duration,
                sourceType = LockSourceType.MOOD,
                sourceId = mood.mood.id
            )
        ).toTemplateResult()
    }

    private suspend fun validateApps(packageIds: Set<String>): String? {
        packageIds.forEach { packageId ->
            val app = appRepository.getApp(packageId)
                ?: return "App is unavailable: $packageId"
            runCatching { AppEligibilityRules.requireLockable(app) }
                .onFailure { return it.message ?: "App cannot be locked." }
        }
        return null
    }

    private fun LockUseCaseResult.toTemplateResult(): TemplateUseCaseResult =
        when (this) {
            is LockUseCaseResult.Created -> TemplateUseCaseResult.Started(lockId)
            is LockUseCaseResult.Extended -> TemplateUseCaseResult.Started(lockId)
            is LockUseCaseResult.Rejected -> TemplateUseCaseResult.Rejected(reason)
        }

    private fun Set<String>.distinctSorted(): List<String> =
        distinct().sorted()
}

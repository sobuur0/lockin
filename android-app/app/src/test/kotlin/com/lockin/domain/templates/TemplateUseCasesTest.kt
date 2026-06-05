package com.lockin.domain.templates

import com.lockin.data.entities.LockSourceType
import com.lockin.domain.lock.LockDuration
import com.lockin.domain.lock.LockUseCases
import com.lockin.testsupport.FakeAppRepository
import com.lockin.testsupport.FakeDeviceOwnerState
import com.lockin.testsupport.FakeLockRepository
import com.lockin.testsupport.FakePolicyEnforcer
import com.lockin.testsupport.FakeTemplateRepository
import com.lockin.testsupport.FakeTimeProvider
import com.lockin.testsupport.lockableApp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateUseCasesTest {
    @Test
    fun rejectsGroupWithBlankName() = runTest {
        val result = useCases().saveGroup(
            SaveLockGroupRequest(
                name = "   ",
                packageIds = setOf("com.example.one")
            )
        )

        assertEquals(
            TemplateUseCaseResult.Rejected("Group name is required."),
            result
        )
    }

    @Test
    fun startsLockFromGroupWithoutMutatingWhenGroupIsEditedLater() = runTest {
        val lockRepository = FakeLockRepository()
        val templates = useCases(lockRepository = lockRepository)

        val saved = templates.saveGroup(
            SaveLockGroupRequest(
                name = "Deep Work",
                packageIds = setOf("com.example.one")
            )
        ) as TemplateUseCaseResult.Saved
        val started = templates.startLockFromGroup(
            StartLockFromGroupRequest(
                groupId = saved.templateId,
                duration = LockDuration.fromMinutes(10)
            )
        )
        templates.saveGroup(
            SaveLockGroupRequest(
                id = saved.templateId,
                name = "Deep Work",
                packageIds = setOf("com.example.two")
            )
        )

        val lock = lockRepository.getLockWithApplications(1L)

        assertEquals(TemplateUseCaseResult.Started(lockId = 1L), started)
        assertEquals(LockSourceType.GROUP, lock?.lock?.sourceType)
        assertEquals(saved.templateId, lock?.lock?.sourceId)
        assertEquals(listOf("com.example.one"), lock?.applications?.map { it.packageId })
    }

    @Test
    fun startsLockFromMoodUsingDefaultDurationWhenNoDurationOverrideIsProvided() = runTest {
        val lockRepository = FakeLockRepository()
        val timeProvider = FakeTimeProvider(wallTime = 1_000L, elapsedTime = 1_000L)
        val templates = useCases(
            lockRepository = lockRepository,
            timeProvider = timeProvider
        )

        val saved = templates.saveMood(
            SaveMoodRequest(
                name = "Study",
                packageIds = setOf("com.example.one"),
                defaultDuration = LockDuration.fromMinutes(15)
            )
        ) as TemplateUseCaseResult.Saved
        val started = templates.startLockFromMood(
            StartLockFromMoodRequest(moodId = saved.templateId)
        )

        val lock = lockRepository.getLock(1L)

        assertEquals(TemplateUseCaseResult.Started(lockId = 1L), started)
        assertEquals(LockSourceType.MOOD, lock?.sourceType)
        assertEquals(saved.templateId, lock?.sourceId)
        assertEquals(LockDuration.fromMinutes(15).millis, lock?.remainingDurationAtLastCheckpoint)
    }

    @Test
    fun rejectsMoodStartWithoutDefaultDurationOrOverride() = runTest {
        val templates = useCases()
        val saved = templates.saveMood(
            SaveMoodRequest(
                name = "Study",
                packageIds = setOf("com.example.one")
            )
        ) as TemplateUseCaseResult.Saved

        val result = templates.startLockFromMood(
            StartLockFromMoodRequest(moodId = saved.templateId)
        )

        assertTrue(result is TemplateUseCaseResult.Rejected)
        assertEquals(
            "Choose a duration before starting this mood.",
            (result as TemplateUseCaseResult.Rejected).reason
        )
    }

    private fun useCases(
        appRepository: FakeAppRepository = FakeAppRepository(
            listOf(
                lockableApp("com.example.one"),
                lockableApp("com.example.two")
            )
        ),
        lockRepository: FakeLockRepository = FakeLockRepository(),
        templateRepository: FakeTemplateRepository = FakeTemplateRepository(),
        timeProvider: FakeTimeProvider = FakeTimeProvider(),
        policyEnforcer: FakePolicyEnforcer = FakePolicyEnforcer()
    ): TemplateUseCases {
        val lockUseCases = LockUseCases(
            appRepository = appRepository,
            lockRepository = lockRepository,
            deviceOwnerState = FakeDeviceOwnerState(),
            timeProvider = timeProvider,
            policyEnforcer = policyEnforcer
        )
        return TemplateUseCases(
            appRepository = appRepository,
            templateRepository = templateRepository,
            lockUseCases = lockUseCases,
            timeProvider = timeProvider
        )
    }
}

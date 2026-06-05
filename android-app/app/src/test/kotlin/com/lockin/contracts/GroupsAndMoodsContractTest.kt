package com.lockin.contracts

import com.lockin.domain.lock.LockDuration
import com.lockin.domain.lock.LockUseCases
import com.lockin.domain.templates.SaveLockGroupRequest
import com.lockin.domain.templates.SaveMoodRequest
import com.lockin.domain.templates.StartLockFromGroupRequest
import com.lockin.domain.templates.StartLockFromMoodRequest
import com.lockin.domain.templates.TemplateUseCaseResult
import com.lockin.domain.templates.TemplateUseCases
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

class GroupsAndMoodsContractTest {
    @Test
    fun groupRequiresNameAndApps() = runTest {
        val templates = useCases()

        assertEquals(
            TemplateUseCaseResult.Rejected("Group name is required."),
            templates.saveGroup(SaveLockGroupRequest(name = "", packageIds = setOf("com.example.one")))
        )
        assertEquals(
            TemplateUseCaseResult.Rejected("Select at least one app for this group."),
            templates.saveGroup(SaveLockGroupRequest(name = "Deep Work", packageIds = emptySet()))
        )
    }

    @Test
    fun moodRequiresNameAppsAndPositiveDefaultDurationWhenPresent() = runTest {
        val templates = useCases()

        assertEquals(
            TemplateUseCaseResult.Rejected("Mood name is required."),
            templates.saveMood(SaveMoodRequest(name = "", packageIds = setOf("com.example.one")))
        )
        assertEquals(
            TemplateUseCaseResult.Rejected("Select at least one app for this mood."),
            templates.saveMood(SaveMoodRequest(name = "Study", packageIds = emptySet()))
        )
    }

    @Test
    fun startFromGroupCreatesActiveLockAfterFinalDurationSelection() = runTest {
        val templates = useCases()
        val saved = templates.saveGroup(
            SaveLockGroupRequest(
                name = "Deep Work",
                packageIds = setOf("com.example.one", "com.example.two")
            )
        ) as TemplateUseCaseResult.Saved

        val result = templates.startLockFromGroup(
            StartLockFromGroupRequest(
                groupId = saved.templateId,
                duration = LockDuration.fromMinutes(20)
            )
        )

        assertEquals(TemplateUseCaseResult.Started(lockId = 1L), result)
    }

    @Test
    fun startFromMoodUsesDefaultDurationOrExplicitOverride() = runTest {
        val templates = useCases()
        val saved = templates.saveMood(
            SaveMoodRequest(
                name = "Study",
                packageIds = setOf("com.example.one"),
                defaultDuration = LockDuration.fromMinutes(20)
            )
        ) as TemplateUseCaseResult.Saved

        val result = templates.startLockFromMood(
            StartLockFromMoodRequest(
                moodId = saved.templateId,
                durationOverride = LockDuration.fromMinutes(5)
            )
        )

        assertTrue(result is TemplateUseCaseResult.Started)
    }

    private fun useCases(): TemplateUseCases {
        val appRepository = FakeAppRepository(
            listOf(
                lockableApp("com.example.one"),
                lockableApp("com.example.two")
            )
        )
        val lockUseCases = LockUseCases(
            appRepository = appRepository,
            lockRepository = FakeLockRepository(),
            deviceOwnerState = FakeDeviceOwnerState(),
            timeProvider = FakeTimeProvider(),
            policyEnforcer = FakePolicyEnforcer()
        )
        return TemplateUseCases(
            appRepository = appRepository,
            templateRepository = FakeTemplateRepository(),
            lockUseCases = lockUseCases,
            timeProvider = FakeTimeProvider()
        )
    }
}

package com.lockin.ui.templates

import com.lockin.domain.lock.LockDurationUnit
import com.lockin.domain.lock.LockUseCases
import com.lockin.domain.templates.TemplateUseCases
import com.lockin.testsupport.FakeAppRepository
import com.lockin.testsupport.FakeDeviceOwnerState
import com.lockin.testsupport.FakeLockRepository
import com.lockin.testsupport.FakePolicyEnforcer
import com.lockin.testsupport.FakeTemplateRepository
import com.lockin.testsupport.FakeTimeProvider
import com.lockin.testsupport.MainDispatcherRule
import com.lockin.testsupport.lockableApp
import com.lockin.ui.groups.GroupsViewModel
import com.lockin.ui.moods.MoodsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class TemplatesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun groupsViewModelSavesGroupAndStartsLock() = runTest {
        val viewModels = viewModels()
        val groups = viewModels.groups

        groups.setName("Deep Work")
        groups.toggleApp("com.example.one")
        groups.saveGroup()
        advanceUntilIdle()

        assertEquals("Deep Work", groups.uiState.value.groups.single().name)
        assertNull(groups.uiState.value.errorMessage)

        groups.setStartDurationAmount("10")
        groups.setStartDurationUnit(LockDurationUnit.MINUTES)
        groups.setStartConfirmationAccepted(true)
        groups.startGroup(groups.uiState.value.groups.single().id)
        advanceUntilIdle()

        assertEquals(1L, groups.uiState.value.startedLockId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun moodsViewModelSavesMoodWithDefaultDurationAndStartsLock() = runTest {
        val viewModels = viewModels()
        val moods = viewModels.moods

        moods.setName("Study")
        moods.toggleApp("com.example.one")
        moods.setDefaultDurationAmount("15")
        moods.setDefaultDurationUnit(LockDurationUnit.MINUTES)
        moods.saveMood()
        advanceUntilIdle()

        assertEquals("Study", moods.uiState.value.moods.single().name)
        assertEquals("15 min", moods.uiState.value.moods.single().defaultDurationLabel)

        moods.setStartConfirmationAccepted(true)
        moods.startMood(moods.uiState.value.moods.single().id)
        advanceUntilIdle()

        assertEquals(1L, moods.uiState.value.startedLockId)
    }

    private data class ViewModels(
        val groups: GroupsViewModel,
        val moods: MoodsViewModel
    )

    private fun viewModels(): ViewModels {
        val appRepository = FakeAppRepository(
            listOf(
                lockableApp("com.example.one").copy(displayName = "One"),
                lockableApp("com.example.two").copy(displayName = "Two")
            )
        )
        val templateRepository = FakeTemplateRepository()
        val lockUseCases = LockUseCases(
            appRepository = appRepository,
            lockRepository = FakeLockRepository(),
            deviceOwnerState = FakeDeviceOwnerState(),
            timeProvider = FakeTimeProvider(),
            policyEnforcer = FakePolicyEnforcer()
        )
        val templateUseCases = TemplateUseCases(
            appRepository = appRepository,
            templateRepository = templateRepository,
            lockUseCases = lockUseCases,
            timeProvider = FakeTimeProvider()
        )
        return ViewModels(
            groups = GroupsViewModel(
                appRepository = appRepository,
                templateRepository = templateRepository,
                templateUseCases = templateUseCases
            ),
            moods = MoodsViewModel(
                appRepository = appRepository,
                templateRepository = templateRepository,
                templateUseCases = templateUseCases
            )
        )
    }
}

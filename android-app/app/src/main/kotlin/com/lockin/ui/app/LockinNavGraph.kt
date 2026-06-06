package com.lockin.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lockin.LockinApp
import com.lockin.app.LockinContainer
import com.lockin.ui.deviceowner.DeviceOwnerGateScreen
import com.lockin.ui.deviceowner.DeviceOwnerGateViewModel
import com.lockin.ui.groups.GroupsScreen
import com.lockin.ui.groups.GroupsViewModel
import com.lockin.ui.home.HomeScreen
import com.lockin.ui.home.HomeViewModel
import com.lockin.ui.lockcreate.CreateLockScreen
import com.lockin.ui.lockcreate.CreateLockViewModel
import com.lockin.ui.lockdetail.LockDetailScreen
import com.lockin.ui.lockdetail.LockDetailViewModel
import com.lockin.ui.moods.MoodsScreen
import com.lockin.ui.moods.MoodsViewModel
import com.lockin.ui.stats.StatsScreen
import com.lockin.ui.stats.StatsViewModel

object LockinRoutes {
    const val DEVICE_OWNER = "device-owner"
    const val HOME = "home"
    const val CREATE_LOCK = "create-lock"
    const val LOCK_DETAIL = "lock-detail/{lockId}"
    const val GROUPS = "groups"
    const val MOODS = "moods"
    const val STATS = "stats"

    fun lockDetail(lockId: Long): String = "lock-detail/$lockId"
}

@Composable
fun LockinNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = LockinRoutes.DEVICE_OWNER
) {
    val context = LocalContext.current
    val container = remember(context) {
        (context.applicationContext as LockinApp).container
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(LockinRoutes.DEVICE_OWNER) {
            val viewModel: DeviceOwnerGateViewModel = viewModel(
                factory = lockinViewModelFactory {
                    DeviceOwnerGateViewModel(container.deviceOwnerState)
                }
            )
            val state by viewModel.uiState.collectAsState()
            DeviceOwnerGateScreen(
                state = state,
                onVerify = viewModel::verifyDeviceOwnerStatus,
                onContinue = {
                    navController.navigate(LockinRoutes.HOME) {
                        popUpTo(LockinRoutes.DEVICE_OWNER)
                    }
                }
            )
        }
        composable(LockinRoutes.HOME) {
            val viewModel: HomeViewModel = viewModel(
                factory = lockinViewModelFactory {
                    HomeViewModel(
                        lockRepository = container.lockRepository,
                        appRepository = container.appRepository,
                        timeProvider = container.timeProvider
                    )
                }
            )
            val state by viewModel.uiState.collectAsState()
            HomeScreen(
                state = state,
                onCreateLock = { navController.navigate(LockinRoutes.CREATE_LOCK) },
                onGroups = { navController.navigate(LockinRoutes.GROUPS) },
                onMoods = { navController.navigate(LockinRoutes.MOODS) },
                onStats = { navController.navigate(LockinRoutes.STATS) },
                onLockSelected = { lockId -> navController.navigate(LockinRoutes.lockDetail(lockId)) }
            )
        }
        composable(LockinRoutes.CREATE_LOCK) {
            val viewModel: CreateLockViewModel = viewModel(
                factory = lockinViewModelFactory {
                    CreateLockViewModel(
                        appRepository = container.appRepository,
                        appCatalogScanner = container.appCatalogScanner,
                        lockUseCases = container.lockUseCases
                    )
                }
            )
            val state by viewModel.uiState.collectAsState()
            CreateLockScreen(
                state = state,
                onToggleApp = viewModel::toggleApp,
                onDurationAmountChange = viewModel::setDurationAmount,
                onDurationUnitChange = viewModel::setDurationUnit,
                onConfirmationChange = viewModel::setConfirmationAccepted,
                onCreateLock = viewModel::createLock,
                onLockCreated = { lockId ->
                    navController.navigate(LockinRoutes.lockDetail(lockId)) {
                        popUpTo(LockinRoutes.CREATE_LOCK) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable(
            route = LockinRoutes.LOCK_DETAIL,
            arguments = listOf(navArgument("lockId") { type = NavType.LongType })
        ) { entry ->
            val lockId = entry.arguments?.getLong("lockId") ?: return@composable
            val viewModel: LockDetailViewModel = viewModel(
                key = "lock-detail-$lockId",
                factory = lockinViewModelFactory {
                    LockDetailViewModel(
                        lockId = lockId,
                        lockRepository = container.lockRepository,
                        appRepository = container.appRepository,
                        lockUseCases = container.lockUseCases,
                        timeProvider = container.timeProvider
                    )
                }
            )
            val state by viewModel.uiState.collectAsState()
            LockDetailScreen(
                state = state,
                onExtensionAmountChange = viewModel::setExtensionAmount,
                onExtensionUnitChange = viewModel::setExtensionUnit,
                onExtensionConfirmationChange = viewModel::setExtensionConfirmed,
                onExtendLock = viewModel::extendLock,
                onRefresh = viewModel::refresh
            )
        }
        composable(LockinRoutes.GROUPS) {
            val viewModel: GroupsViewModel = viewModel(
                factory = lockinViewModelFactory {
                    GroupsViewModel(
                        appRepository = container.appRepository,
                        templateRepository = container.templateRepository,
                        templateUseCases = container.templateUseCases
                    )
                }
            )
            val state by viewModel.uiState.collectAsState()
            GroupsScreen(
                state = state,
                onNameChange = viewModel::setName,
                onToggleApp = viewModel::toggleApp,
                onSaveGroup = viewModel::saveGroup,
                onEditGroup = viewModel::editGroup,
                onArchiveGroup = viewModel::archiveGroup,
                onClearEditor = viewModel::clearEditor,
                onStartDurationAmountChange = viewModel::setStartDurationAmount,
                onStartDurationUnitChange = viewModel::setStartDurationUnit,
                onStartConfirmationChange = viewModel::setStartConfirmationAccepted,
                onStartGroup = viewModel::startGroup,
                onLockStarted = { lockId ->
                    navController.navigate(LockinRoutes.lockDetail(lockId))
                }
            )
        }
        composable(LockinRoutes.MOODS) {
            val viewModel: MoodsViewModel = viewModel(
                factory = lockinViewModelFactory {
                    MoodsViewModel(
                        appRepository = container.appRepository,
                        templateRepository = container.templateRepository,
                        templateUseCases = container.templateUseCases
                    )
                }
            )
            val state by viewModel.uiState.collectAsState()
            MoodsScreen(
                state = state,
                onNameChange = viewModel::setName,
                onToggleApp = viewModel::toggleApp,
                onDefaultDurationAmountChange = viewModel::setDefaultDurationAmount,
                onDefaultDurationUnitChange = viewModel::setDefaultDurationUnit,
                onSaveMood = viewModel::saveMood,
                onEditMood = viewModel::editMood,
                onArchiveMood = viewModel::archiveMood,
                onClearEditor = viewModel::clearEditor,
                onStartDurationAmountChange = viewModel::setStartDurationAmount,
                onStartDurationUnitChange = viewModel::setStartDurationUnit,
                onStartConfirmationChange = viewModel::setStartConfirmationAccepted,
                onStartMood = viewModel::startMood,
                onLockStarted = { lockId ->
                    navController.navigate(LockinRoutes.lockDetail(lockId))
                }
            )
        }
        composable(LockinRoutes.STATS) {
            val viewModel: StatsViewModel = viewModel(
                factory = lockinViewModelFactory {
                    StatsViewModel(container.statisticsRepository)
                }
            )
            val state by viewModel.uiState.collectAsState()
            StatsScreen(state = state)
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

private fun <VM : ViewModel> lockinViewModelFactory(
    create: () -> VM
): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
    }

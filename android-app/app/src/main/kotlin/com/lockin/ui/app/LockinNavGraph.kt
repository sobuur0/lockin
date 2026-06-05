package com.lockin.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import com.lockin.ui.lockcreate.CreateLockScreen
import com.lockin.ui.lockcreate.CreateLockViewModel
import com.lockin.ui.lockdetail.LockDetailScreen
import com.lockin.ui.lockdetail.LockDetailViewModel

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
            HomeScreen(
                onCreateLock = { navController.navigate(LockinRoutes.CREATE_LOCK) }
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
                onExtendLock = viewModel::extendLock
            )
        }
        composable(LockinRoutes.GROUPS) {
            PlaceholderScreen(title = "Groups")
        }
        composable(LockinRoutes.MOODS) {
            PlaceholderScreen(title = "Moods")
        }
        composable(LockinRoutes.STATS) {
            PlaceholderScreen(title = "Statistics")
        }
    }
}

@Composable
private fun HomeScreen(
    onCreateLock: () -> Unit
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Lockin",
                style = MaterialTheme.typography.headlineSmall
            )
            Button(
                onClick = onCreateLock,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Lock")
            }
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

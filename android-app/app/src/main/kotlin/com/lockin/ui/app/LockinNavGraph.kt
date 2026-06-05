package com.lockin.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

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
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(LockinRoutes.DEVICE_OWNER) {
            PlaceholderScreen(title = "Device Owner")
        }
        composable(LockinRoutes.HOME) {
            PlaceholderScreen(title = "Home")
        }
        composable(LockinRoutes.CREATE_LOCK) {
            PlaceholderScreen(title = "Create Lock")
        }
        composable(LockinRoutes.LOCK_DETAIL) {
            PlaceholderScreen(title = "Lock Detail")
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

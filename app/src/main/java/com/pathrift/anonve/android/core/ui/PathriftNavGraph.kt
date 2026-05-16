package com.pathrift.anonve.android.core.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.pathrift.anonve.android.core.storage.LocalProgressStore
import com.pathrift.anonve.android.core.ui.screens.ArsenalScreen
import com.pathrift.anonve.android.core.ui.screens.GameScreen
import com.pathrift.anonve.android.core.ui.screens.HomeScreen
import com.pathrift.anonve.android.core.ui.screens.HowToPlayScreen
import com.pathrift.anonve.android.core.ui.screens.RunEndScreen
import com.pathrift.anonve.android.core.ui.screens.SettingsScreen
import com.pathrift.anonve.android.core.ui.screens.StoreScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Game : Screen("game")
    object RunEnd : Screen("run_end/{score}/{wave}/{kills}/{isVictory}") {
        fun createRoute(score: Long, wave: Int, kills: Int, isVictory: Boolean) =
            "run_end/$score/$wave/$kills/${if (isVictory) 1 else 0}"
    }
    object Settings : Screen("settings")
    object Store : Screen("store")
    object HowToPlay : Screen("how_to_play")
    object Arsenal : Screen("arsenal")
}

@Composable
fun PathriftNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(
            route = Screen.Home.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() }
        ) {
            HomeScreen(
                onStartGame = {
                    navController.navigate(Screen.Game.route)
                },
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onOpenStore = {
                    navController.navigate(Screen.Store.route)
                },
                onOpenHowToPlay = {
                    navController.navigate(Screen.HowToPlay.route)
                },
                onOpenArsenal = {
                    navController.navigate(Screen.Arsenal.route)
                }
            )
        }

        composable(
            route = Screen.Game.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) {
            GameScreen(
                onRunEnded = { score, wave, kills ->
                    navController.navigate(Screen.RunEnd.createRoute(score, wave, kills, isVictory = false)) {
                        popUpTo(Screen.Game.route) { inclusive = true }
                    }
                },
                navController = navController
            )
        }

        composable(
            route = Screen.RunEnd.route,
            arguments = listOf(
                navArgument("score") { type = NavType.LongType },
                navArgument("wave") { type = NavType.IntType },
                navArgument("kills") { type = NavType.IntType },
                navArgument("isVictory") { type = NavType.IntType }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) { backStackEntry ->
            val score = backStackEntry.arguments?.getLong("score") ?: 0L
            val wave = backStackEntry.arguments?.getInt("wave") ?: 0
            val kills = backStackEntry.arguments?.getInt("kills") ?: 0
            val isVictory = (backStackEntry.arguments?.getInt("isVictory") ?: 0) == 1
            val context = LocalContext.current
            val progressStore = remember { LocalProgressStore(context) }
            val previousBestScore by progressStore.bestScore.collectAsState(initial = 0L)
            RunEndScreen(
                score = score,
                wave = wave,
                enemyKills = kills,
                isVictory = isVictory,
                previousBestScore = previousBestScore,
                onPlayAgain = {
                    navController.navigate(Screen.Game.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onMainMenu = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Settings.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) {
            SettingsScreen(
                onBack = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Store.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) {
            StoreScreen(
                onBack = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Arsenal.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) {
            ArsenalScreen(
                onBack = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.HowToPlay.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) {
            HowToPlayScreen(
                onBack = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

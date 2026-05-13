package com.pathrift.anonve.android.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
    object RunEnd : Screen("run_end/{score}/{wave}") {
        fun createRoute(score: Long, wave: Int) = "run_end/$score/$wave"
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
        composable(Screen.Home.route) {
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

        composable(Screen.Game.route) {
            GameScreen(
                onRunEnded = { score, wave ->
                    navController.navigate(Screen.RunEnd.createRoute(score, wave)) {
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
                navArgument("wave") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val score = backStackEntry.arguments?.getLong("score") ?: 0L
            val wave = backStackEntry.arguments?.getInt("wave") ?: 0
            RunEndScreen(
                score = score,
                wave = wave,
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

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Store.route) {
            StoreScreen(
                onBack = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Arsenal.route) {
            ArsenalScreen(
                onBack = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.HowToPlay.route) {
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

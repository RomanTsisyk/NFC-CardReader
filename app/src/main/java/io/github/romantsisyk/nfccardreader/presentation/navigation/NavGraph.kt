package io.github.romantsisyk.nfccardreader.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.romantsisyk.nfccardreader.presentation.ui.HistoryScreen
import io.github.romantsisyk.nfccardreader.presentation.ui.NFCReaderScreen
import io.github.romantsisyk.nfccardreader.presentation.viewmodel.NFCReaderViewModel

/**
 * Navigation routes for the application.
 */
object NavRoutes {
    const val READER = "reader"
    const val HISTORY = "history"
}

/**
 * Main navigation graph for the application.
 *
 * @param navController The navigation controller
 * @param viewModel The shared ViewModel
 */
@Composable
fun NfcNavGraph(
    navController: NavHostController,
    viewModel: NFCReaderViewModel
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.READER
    ) {
        composable(NavRoutes.READER) {
            NFCReaderScreen(
                viewModel = viewModel,
                onNavigateToHistory = {
                    navController.navigate(NavRoutes.HISTORY)
                }
            )
        }

        composable(NavRoutes.HISTORY) {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

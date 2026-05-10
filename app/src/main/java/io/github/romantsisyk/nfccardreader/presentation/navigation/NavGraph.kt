package io.github.romantsisyk.nfccardreader.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.romantsisyk.nfccardreader.presentation.ui.HistoryScreen
import io.github.romantsisyk.nfccardreader.presentation.ui.NFCReaderScreen
import io.github.romantsisyk.nfccardreader.presentation.viewmodel.NFCReaderViewModel

object NavRoutes {
    const val READER = "reader"
    const val HISTORY = "history"
}

@Composable
fun NfcNavGraph(navController: NavHostController) {
    val viewModel: NFCReaderViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.READER
    ) {
        composable(NavRoutes.READER) {
            val uiState by viewModel.uiState.collectAsState()
            NFCReaderScreen(
                uiState = uiState,
                onClearData = viewModel::clearNfcData,
                onSaveScan = viewModel::saveCurrentScan,
                onDismissError = viewModel::dismissError,
                onNavigateToHistory = {
                    navController.navigate(NavRoutes.HISTORY) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(NavRoutes.HISTORY) {
            val history by viewModel.scanHistory.collectAsState()
            HistoryScreen(
                history = history,
                onDelete = viewModel::deleteScan,
                onClearAll = viewModel::clearHistory,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

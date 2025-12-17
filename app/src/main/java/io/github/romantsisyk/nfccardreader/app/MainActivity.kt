package io.github.romantsisyk.nfccardreader.app

import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.romantsisyk.nfccardreader.presentation.navigation.NfcNavGraph
import io.github.romantsisyk.nfccardreader.presentation.ui.theme.NFCCardReaderTheme
import io.github.romantsisyk.nfccardreader.presentation.viewmodel.NFCReaderViewModel
import kotlinx.coroutines.launch

/**
 * Main activity for the NFC Card Reader application.
 *
 * Handles NFC intent discovery and hosts the navigation graph.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: NFCReaderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NFCCardReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NfcNavGraph(
                        navController = navController,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkNfcAvailability()
        handleNfcIntent()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent()
    }

    private fun handleNfcIntent() {
        intent?.let {
            Log.d("MainActivity", "Intent action: ${it.action}")
            if (it.action == NfcAdapter.ACTION_TAG_DISCOVERED ||
                it.action == NfcAdapter.ACTION_TECH_DISCOVERED ||
                it.action == NfcAdapter.ACTION_NDEF_DISCOVERED
            ) {
                lifecycleScope.launch {
                    try {
                        viewModel.processNfcIntent(it)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error processing NFC intent", e)
                    }
                }
            }
        }
    }
}

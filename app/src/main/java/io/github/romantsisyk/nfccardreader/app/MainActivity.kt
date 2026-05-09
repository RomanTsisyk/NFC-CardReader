package io.github.romantsisyk.nfccardreader.app

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.romantsisyk.nfccardreader.presentation.navigation.NfcNavGraph
import io.github.romantsisyk.nfccardreader.presentation.ui.theme.NFCCardReaderTheme
import io.github.romantsisyk.nfccardreader.presentation.viewmodel.NFCReaderViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Used only for NFC lifecycle callbacks — UI state flows from hiltViewModel() in NavGraph.
    private val viewModel: NFCReaderViewModel by viewModels()

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var nfcPendingIntent: PendingIntent
    private val nfcIntentFilters = arrayOf(
        IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
        IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
        IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Prevent screenshots/screen-capture of card data
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcPendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        setContent {
            NFCCardReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NfcNavGraph(navController = rememberNavController())
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkNfcAvailability()
        // Enable foreground dispatch so taps go to onNewIntent, not a new Activity instance
        nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, nfcIntentFilters, null)
        handleNfcIntent()
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent()
    }

    private fun handleNfcIntent() {
        val currentIntent = intent ?: return
        if (currentIntent.action in NFC_ACTIONS) {
            viewModel.processNfcIntent(currentIntent)
            // Consume the intent so onResume doesn't re-fire it on next foreground
            setIntent(Intent())
        }
    }

    companion object {
        private val NFC_ACTIONS = setOf(
            NfcAdapter.ACTION_TAG_DISCOVERED,
            NfcAdapter.ACTION_TECH_DISCOVERED,
            NfcAdapter.ACTION_NDEF_DISCOVERED
        )
    }
}

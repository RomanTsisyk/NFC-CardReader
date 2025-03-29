package io.github.romantsisyk.nfccardreader.app

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcA
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.romantsisyk.nfccardreader.presentation.ui.NFCReaderUI
import io.github.romantsisyk.nfccardreader.presentation.viewmodel.NFCReaderViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: NFCReaderViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private val intentFiltersArray: Array<IntentFilter>? = null
    private val techListsArray = arrayOf(arrayOf(IsoDep::class.java.name), arrayOf(NfcA::class.java.name))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NFCReaderUI(viewModel)
        }

        // Initialize NFC adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "This device doesn't support NFC", Toast.LENGTH_LONG).show()
            return
        }

        // Create a PendingIntent that will be used to read NFC tags
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        // Process any intent that might have launched the activity
        processIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Enable NFC foreground dispatch to intercept NFC tags while the app is in the foreground
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
    }

    override fun onPause() {
        super.onPause()
        // Disable NFC foreground dispatch when app goes into background
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Set the new intent to process it
        setIntent(intent)
        // Process the new intent
        processIntent(intent)
    }

    private fun processIntent(intent: Intent) {
        val action = intent.action
        Log.d("MainActivity", "Intent action: $action")

        if (NfcAdapter.ACTION_TAG_DISCOVERED == action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == action) {

            lifecycleScope.launch {
                try {
                    viewModel.processNfcIntent(intent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error processing NFC intent", e)
                    val errorMessage = when {
                        e.message?.contains("out of date") == true ->
                            "The NFC tag was moved too quickly. Please hold it steady against the device."
                        e.message?.contains("Transceive failed") == true ->
                            "Communication with the card failed. Please try again."
                        else -> "Error reading NFC card: ${e.message}"
                    }
                    viewModel.setError(errorMessage)
                    Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
package io.github.romantsisyk.nfccardreader

import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: NFCReaderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NFCReaderScreen(viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        intent?.let {
            if (it.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
                Log.d("NFCReader", "NFC Intent Detected in onResume")
                lifecycleScope.launch {
                    viewModel.processNfcIntent(it)
                }
            }
        }
        Log.d("NFCReader", "onResume was Triggered")
    }
}

@Composable
fun NFCReaderScreen(viewModel: NFCReaderViewModel) {
    val nfcData = viewModel.nfcTagData.collectAsState(initial = emptyMap())
    val rawResponse = viewModel.rawResponse.collectAsState(initial = "")
    val error = viewModel.error.collectAsState(initial = null)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "NFC Reader", fontSize = 24.sp, modifier = Modifier.padding(16.dp))

                if (nfcData.value.isNotEmpty()) {
                    nfcData.value.forEach { (key, value) ->
                        Text(text = "$key: $value", fontSize = 16.sp, modifier = Modifier.padding(8.dp))
                    }
                } else {
                    Text(
                        text = "No NFC Data Available",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                if (rawResponse.value.isNotBlank()) {
                    Text(
                        text = "Raw Response: ${rawResponse.value}",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                error.value?.let {
                    Text(
                        text = "Error: $it",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Button(
                    onClick = { viewModel.clearNfcData() },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Clear Data")
                }
            }
        }
    )
}

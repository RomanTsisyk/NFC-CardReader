package io.github.romantsisyk.nfccardreader.presentation.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.romantsisyk.nfccardreader.R
import io.github.romantsisyk.nfccardreader.presentation.viewmodel.NFCReaderViewModel
import io.github.romantsisyk.nfccardreader.presentation.viewmodel.NfcUiState
import io.github.romantsisyk.nfccardreader.utils.orNA

/**
 * Main NFC Reader screen composable.
 *
 * @param viewModel The ViewModel containing NFC data and state
 * @param onNavigateToHistory Callback for navigating to history screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCReaderScreen(
    viewModel: NFCReaderViewModel,
    onNavigateToHistory: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Section visibility states
    var showRawResponse by remember { mutableStateOf(true) }
    var showParsedTlvData by remember { mutableStateOf(true) }
    var showBasicCardInfo by remember { mutableStateOf(true) }
    var showAdvancedCardInfo by remember { mutableStateOf(false) }
    var showTransactionInfo by remember { mutableStateOf(true) }
    var showSecurityInfo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nfc_reader_card_information)) },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // NFC Status Banner
                NfcStatusBanner(uiState)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Action Buttons
                    item {
                        ActionButtonsRow(
                            uiState = uiState,
                            onClear = { viewModel.clearNfcData() },
                            onLoadMock = { viewModel.processMockNfcIntent() },
                            onSave = { viewModel.saveCurrentScan() }
                        )
                    }

                    // Raw NFC Response
                    item {
                        CollapsibleCard(
                            title = stringResource(R.string.raw_nfc_response),
                            icon = Icons.Default.Terminal,
                            isExpanded = showRawResponse,
                            onToggle = { showRawResponse = !showRawResponse }
                        ) {
                            Text(
                                text = stringResource(R.string.raw_response, uiState.rawResponse),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    // Basic Card Information
                    item {
                        CollapsibleCard(
                            title = "Basic Card Information",
                            icon = Icons.Default.CreditCard,
                            isExpanded = showBasicCardInfo,
                            onToggle = { showBasicCardInfo = !showBasicCardInfo }
                        ) {
                            BasicCardInfoContent(uiState)
                        }
                    }

                    // Transaction Information
                    item {
                        CollapsibleCard(
                            title = "Transaction Information",
                            icon = Icons.Default.Paid,
                            isExpanded = showTransactionInfo,
                            onToggle = { showTransactionInfo = !showTransactionInfo }
                        ) {
                            TransactionInfoContent(uiState)
                        }
                    }

                    // Advanced Card Information
                    item {
                        CollapsibleCard(
                            title = "Advanced Card Information",
                            icon = Icons.Default.Info,
                            isExpanded = showAdvancedCardInfo,
                            onToggle = { showAdvancedCardInfo = !showAdvancedCardInfo }
                        ) {
                            AdvancedCardInfoContent(uiState)
                        }
                    }

                    // Security Information
                    item {
                        CollapsibleCard(
                            title = "Security Information",
                            icon = Icons.Default.Lock,
                            isExpanded = showSecurityInfo,
                            onToggle = { showSecurityInfo = !showSecurityInfo }
                        ) {
                            SecurityInfoContent(uiState)
                        }
                    }

                    // Parsed TLV Data
                    item {
                        CollapsibleCard(
                            title = stringResource(R.string.parsed_tlv_data),
                            icon = Icons.Default.AccountCircle,
                            isExpanded = showParsedTlvData,
                            onToggle = { showParsedTlvData = !showParsedTlvData }
                        ) {
                            ParsedTlvContent(uiState)
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }

            // Loading Overlay
            if (uiState.isLoading) {
                LoadingOverlay()
            }
        }
    }
}

/**
 * NFC status banner showing current state.
 */
@Composable
private fun NfcStatusBanner(uiState: NfcUiState) {
    val backgroundColor: Color
    val text: String
    val icon: ImageVector

    when {
        uiState.errorMessage != null -> {
            backgroundColor = MaterialTheme.colorScheme.errorContainer
            text = uiState.errorMessage
            icon = Icons.Default.Error
        }
        uiState.nfcAvailability?.isAvailable == false -> {
            backgroundColor = MaterialTheme.colorScheme.errorContainer
            text = "NFC not available on this device"
            icon = Icons.Default.Warning
        }
        uiState.nfcAvailability?.isEnabled == false -> {
            backgroundColor = Color(0xFFFFF3E0)
            text = "NFC is disabled. Please enable it in settings."
            icon = Icons.Default.Warning
        }
        uiState.lastScanSaved -> {
            backgroundColor = MaterialTheme.colorScheme.primaryContainer
            text = "Scan saved to history"
            icon = Icons.Default.Check
        }
        uiState.additionalInfo != null -> {
            backgroundColor = Color(0xFFE8F5E9)
            text = "Card data read successfully"
            icon = Icons.Default.CheckCircle
        }
        else -> {
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
            text = "Ready to scan NFC card"
            icon = Icons.Default.Nfc
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Row of action buttons.
 */
@Composable
private fun ActionButtonsRow(
    uiState: NfcUiState,
    onClear: () -> Unit,
    onLoadMock: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onClear,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Clear")
        }

        OutlinedButton(
            onClick = onLoadMock,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Mock")
        }

        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            enabled = uiState.additionalInfo != null && !uiState.lastScanSaved,
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save")
        }
    }
}

/**
 * Collapsible card component.
 */
@Composable
private fun CollapsibleCard(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggle() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle"
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    content()
                }
            }
        }
    }
}

/**
 * Basic card information content.
 */
@Composable
private fun BasicCardInfoContent(uiState: NfcUiState) {
    val info = uiState.additionalInfo
    val tlvData = uiState.nfcTagData

    if (info == null && tlvData.isEmpty()) {
        Text("No card information available", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        InfoRow("Card Type", info?.cardType.orNA())
        tlvData["Cardholder Name"]?.let { InfoRow("Cardholder Name", it) }
        tlvData["Application PAN"]?.let { InfoRow("Card Number", it) }
        tlvData["Expiration Date"]?.let { InfoRow("Expiration Date", it) }
        InfoRow("Application Label", info?.applicationLabel.orNA())
        tlvData["Application Preferred Name"]?.let { InfoRow("Preferred Name", it) }
    }
}

/**
 * Transaction information content.
 */
@Composable
private fun TransactionInfoContent(uiState: NfcUiState) {
    val info = uiState.additionalInfo

    if (info == null) {
        Text("No transaction information available", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        InfoRow("Amount", info.transactionAmount.orNA())
        InfoRow("Currency", info.currencyCode.orNA())
        InfoRow("Date", info.transactionDate.orNA())
        InfoRow("Status", info.transactionStatus.orNA())
        info.transactionType?.let { InfoRow("Type", it) }
        info.transactionCategoryCode?.let { InfoRow("Category", it) }
    }
}

/**
 * Advanced card information content.
 */
@Composable
private fun AdvancedCardInfoContent(uiState: NfcUiState) {
    val info = uiState.additionalInfo

    if (info == null) {
        Text("No advanced information available", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        info.applicationIdentifier?.let { InfoRow("Application ID", it) }
        info.applicationTemplate?.let { InfoRow("App Template", it) }
        info.dedicatedFileName?.let { InfoRow("File Name", it) }
        info.issuerCountryCode?.let { InfoRow("Issuer Country", it) }
        info.transactionCurrencyExponent?.let { InfoRow("Currency Exponent", it) }
        info.serviceCode?.let { InfoRow("Service Code", it) }
        info.issuerUrl?.let { InfoRow("Issuer URL", it) }
        info.formFactorIndicator?.let { InfoRow("Form Factor", it) }
        info.terminalCountryCode?.let { InfoRow("Terminal Country", it) }
        info.applicationCurrencyCode?.let { InfoRow("App Currency", it) }
    }
}

/**
 * Security information content.
 */
@Composable
private fun SecurityInfoContent(uiState: NfcUiState) {
    val info = uiState.additionalInfo

    if (info == null) {
        Text("No security information available", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        info.applicationCryptogram?.let { InfoRow("Cryptogram", it) }
        info.applicationTransactionCounter?.let { InfoRow("Transaction Counter", it) }
        info.applicationInterchangeProfile?.let { InfoRow("Interchange Profile", it) }
        info.terminalVerificationResults?.let { InfoRow("Terminal Verification", it) }
        info.cardholderVerificationMethodResults?.let { InfoRow("CVM Method", it) }
        info.issuerScriptResults?.let { InfoRow("Issuer Script Results", it) }
        info.unpredictableNumber?.let { InfoRow("Unpredictable Number", it) }
    }
}

/**
 * Parsed TLV data content.
 */
@Composable
private fun ParsedTlvContent(uiState: NfcUiState) {
    val tlvData = uiState.nfcTagData

    if (tlvData.isEmpty()) {
        Text(stringResource(R.string.no_tlv_data_available), fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        tlvData.forEach { (key, value) ->
            InfoRow(key, value)
        }
    }
}

/**
 * Single row of information with label and value.
 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f)
        )
    }
}

/**
 * Loading overlay with progress indicator.
 */
@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Processing...", fontWeight = FontWeight.Medium)
            }
        }
    }
}

// Legacy composable for backward compatibility
@Composable
fun NFCReaderUI(viewModel: NFCReaderViewModel) {
    NFCReaderScreen(viewModel = viewModel)
}

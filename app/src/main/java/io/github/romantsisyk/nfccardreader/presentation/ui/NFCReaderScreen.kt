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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.romantsisyk.nfccardreader.R
import io.github.romantsisyk.nfccardreader.presentation.viewmodel.NfcUiState
import io.github.romantsisyk.nfccardreader.utils.orNA

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCReaderScreen(
    uiState: NfcUiState,
    onClearData: () -> Unit,
    onSaveScan: () -> Unit,
    onDismissError: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
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
                        Icon(Icons.Default.History, contentDescription = "View scan history")
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
                NfcStatusBanner(uiState, onDismissError)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        ActionButtonsRow(
                            uiState = uiState,
                            onClear = onClearData,
                            onSave = onSaveScan
                        )
                    }

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

            if (uiState.isLoading) {
                LoadingOverlay()
            }
        }
    }
}

@Composable
private fun NfcStatusBanner(uiState: NfcUiState, onDismissError: () -> Unit) {
    val (backgroundColor, text, icon) = remember(uiState) {
        when {
            uiState.errorMessage != null -> Triple(
                null as Color?, uiState.errorMessage, Icons.Default.Error
            )
            uiState.nfcAvailability?.isAvailable == false -> Triple(
                null, "NFC not available on this device", Icons.Default.Warning
            )
            uiState.nfcAvailability?.isEnabled == false -> Triple(
                null, "NFC is disabled. Please enable it in settings.", Icons.Default.Warning
            )
            uiState.lastScanSaved -> Triple(
                null, "Scan saved to history", Icons.Default.Check
            )
            uiState.additionalInfo != null -> Triple(
                null, "Card data read successfully", Icons.Default.CheckCircle
            )
            else -> Triple(null, "Ready to scan NFC card", Icons.Default.Nfc)
        }
    }

    val resolvedBg = when {
        uiState.errorMessage != null || uiState.nfcAvailability?.isAvailable == false ->
            MaterialTheme.colorScheme.errorContainer
        uiState.nfcAvailability?.isEnabled == false ->
            MaterialTheme.colorScheme.tertiaryContainer
        uiState.lastScanSaved ->
            MaterialTheme.colorScheme.primaryContainer
        uiState.additionalInfo != null ->
            MaterialTheme.colorScheme.secondaryContainer
        else ->
            MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(resolvedBg)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text ?: "",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (uiState.errorMessage != null) {
            IconButton(onClick = onDismissError) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss error")
            }
        }
    }
}

@Composable
private fun ActionButtonsRow(
    uiState: NfcUiState,
    onClear: () -> Unit,
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
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Clear")
        }

        Button(
            onClick = onSave,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
            enabled = uiState.additionalInfo != null && !uiState.lastScanSaved,
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save")
        }
    }
}

@Composable
private fun CollapsibleCard(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val toggleDescription = if (isExpanded) "Collapse $title section" else "Expand $title section"

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
                    .semantics {
                        role = Role.Button
                        stateDescription = toggleDescription
                    }
                    .clickable(onClickLabel = toggleDescription) { onToggle() }
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
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = toggleDescription
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

@Composable
private fun BasicCardInfoContent(uiState: NfcUiState) {
    val info = uiState.additionalInfo
    val tlvData = uiState.nfcTagData

    if (info == null && tlvData.isEmpty()) {
        Text("No card information available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        InfoRow("Card Type", info?.cardType.orNA())
        tlvData["CARDHOLDER_NAME"]?.let { InfoRow("Cardholder Name", it) }
        tlvData["APPLICATION_PAN"]?.let { InfoRow("Card Number", it) }
        tlvData["EXPIRATION_DATE"]?.let { InfoRow("Expiration Date", it) }
        InfoRow("Application Label", info?.applicationLabel.orNA())
        tlvData["APPLICATION_PREFERRED_NAME"]?.let { InfoRow("Preferred Name", it) }
    }
}

@Composable
private fun TransactionInfoContent(uiState: NfcUiState) {
    val info = uiState.additionalInfo

    if (info == null) {
        Text("No transaction information available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
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

@Composable
private fun AdvancedCardInfoContent(uiState: NfcUiState) {
    val info = uiState.additionalInfo

    if (info == null) {
        Text("No advanced information available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        info.applicationIdentifier?.let { InfoRow("Application ID", it) }
        info.applicationTemplate?.let { InfoRow("App Template", it) }
        info.dedicatedFileName?.let { InfoRow("File Name", it) }
        info.issuerCountryCode?.let { InfoRow("Issuer Country", it) }
        info.transactionCurrencyExponent?.let { InfoRow("Currency Exponent", it) }
        info.serviceCode?.let { InfoRow("Service Code", it) }
        info.formFactorIndicator?.let { InfoRow("Form Factor", it) }
        info.terminalCountryCode?.let { InfoRow("Terminal Country", it) }
        info.applicationCurrencyCode?.let { InfoRow("App Currency", it) }
    }
}

@Composable
private fun SecurityInfoContent(uiState: NfcUiState) {
    val info = uiState.additionalInfo

    if (info == null) {
        Text("No security information available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
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

@Composable
private fun ParsedTlvContent(uiState: NfcUiState) {
    val tlvData = uiState.nfcTagData

    if (tlvData.isEmpty()) {
        Text(stringResource(R.string.no_tlv_data_available), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        tlvData.forEach { (key, value) ->
            InfoRow(key, value)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f)
        )
    }
}

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

@Preview(showBackground = true)
@Composable
private fun NFCReaderScreenIdlePreview() {
    NFCReaderScreen(
        uiState = NfcUiState(),
        onClearData = {},
        onSaveScan = {},
        onDismissError = {},
        onNavigateToHistory = {}
    )
}

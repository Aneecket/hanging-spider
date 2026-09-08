package com.hangingspider.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hangingspider.game.data.model.CashoutRequest
import com.hangingspider.game.data.model.RewardCatalog
import com.hangingspider.game.data.model.RewardProvider
import com.hangingspider.game.data.model.RewardTier
import com.hangingspider.game.ui.theme.AppColors
import com.hangingspider.game.ui.theme.AppGradients
import com.hangingspider.game.viewmodel.CashoutEvent
import com.hangingspider.game.viewmodel.CashoutViewModel
import com.hangingspider.game.viewmodel.CoinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashoutScreen(
    uid: String,
    email: String,
    coinVm: CoinViewModel,
    onExit: () -> Unit
) {
    val cashVm: CashoutViewModel = viewModel(
        key = "cash-$uid",
        factory = CashoutVmFactory(uid)
    )
    val profile by coinVm.profile.collectAsStateWithLifecycle()
    val requests by cashVm.requests.collectAsStateWithLifecycle()
    val event by cashVm.event.collectAsStateWithLifecycle()

    var pending by remember { mutableStateOf<RewardTier?>(null) }
    var deliveryEmail by remember { mutableStateOf(email) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(event) {
        val e = event ?: return@LaunchedEffect
        val msg = when (e) {
            is CashoutEvent.Submitted -> "Request received. Fulfilled within 48h — watch the Missives tab for your code."
            is CashoutEvent.InsufficientCoins -> "You have ${e.have} coins, need ${e.need}."
            is CashoutEvent.Error -> "Something went wrong: ${e.message}"
        }
        snackbar.showSnackbar(msg)
        cashVm.consumeEvent()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppGradients.homeBackground)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "CASH OUT",
                            style = MaterialTheme.typography.headlineMedium,
                            color = AppColors.GoldBright
                        )
                    },
                    navigationIcon = {
                        TextButton(onClick = onExit) {
                            Text("‹ Back", color = AppColors.Lavender, style = MaterialTheme.typography.labelLarge)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            snackbarHost = {
                SnackbarHost(snackbar) { data ->
                    Snackbar(
                        containerColor = AppColors.Aubergine,
                        contentColor = AppColors.Ivory,
                        shape = RoundedCornerShape(14.dp),
                        snackbarData = data
                    )
                }
            }
        ) { inner ->
            Column(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(4.dp))
                BalanceRow(coins = profile?.coins ?: 0)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Exchange coins for Amazon gift cards. Codes arrive in your Missives tab within 48 hours.",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = AppColors.Lavender
                )
                Spacer(Modifier.height(20.dp))

                RewardProvider.entries.forEach { provider ->
                    ProviderSection(
                        provider = provider,
                        tiers = RewardCatalog.byProvider(provider),
                        currentCoins = profile?.coins ?: 0,
                        onPick = { pending = it }
                    )
                    Spacer(Modifier.height(18.dp))
                }

                if (requests.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "YOUR REQUESTS",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.MutedText,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                    requests.forEach { req -> HistoryRow(req) }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    pending?.let { tier ->
        ConfirmDialog(
            tier = tier,
            email = deliveryEmail,
            onEmailChange = { deliveryEmail = it },
            onConfirm = {
                cashVm.submit(tier.id, deliveryEmail)
                pending = null
            },
            onDismiss = { pending = null }
        )
    }
}

@Composable
private fun BalanceRow(coins: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AppGradients.goldHero)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(AppColors.PlumDeep, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("◉", color = AppColors.GoldBright, fontSize = 22.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                "AVAILABLE",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.PlumDeep.copy(alpha = 0.75f)
            )
            Text(
                "$coins coins",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 22.sp),
                color = AppColors.PlumDeep
            )
        }
    }
}

@Composable
private fun ProviderSection(
    provider: RewardProvider,
    tiers: List<RewardTier>,
    currentCoins: Long,
    onPick: (RewardTier) -> Unit
) {
    val accent = Color(provider.accentHex)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(accent.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(provider.glyph, color = accent, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                provider.display.uppercase(),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 16.sp, letterSpacing = 3.sp),
                color = AppColors.Ivory
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(tiers) { tier -> TierCard(tier = tier, accent = accent, currentCoins = currentCoins, onPick = onPick) }
        }
    }
}

@Composable
private fun TierCard(tier: RewardTier, accent: Color, currentCoins: Long, onPick: (RewardTier) -> Unit) {
    val canAfford = currentCoins >= tier.coins
    Column(
        modifier = Modifier
            .width(120.dp)
            .shadow(6.dp, RoundedCornerShape(18.dp), spotColor = accent.copy(alpha = 0.4f))
            .clip(RoundedCornerShape(18.dp))
            .background(AppColors.Aubergine.copy(alpha = if (canAfford) 0.9f else 0.4f))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "₹${tier.amountInr}",
            style = MaterialTheme.typography.displayMedium.copy(fontSize = 22.sp, letterSpacing = 0.sp),
            color = if (canAfford) accent else AppColors.MutedText
        )
        Text(
            tier.provider.display,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp, letterSpacing = 1.sp),
            color = AppColors.IvoryDim
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "${tier.coins}",
            style = MaterialTheme.typography.titleMedium,
            color = if (canAfford) AppColors.GoldBright else AppColors.MutedText
        )
        Text(
            "coins",
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.sp),
            color = AppColors.MutedText
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (canAfford) AppGradients.violetCta else androidx.compose.ui.graphics.SolidColor(AppColors.CharcoalMid)),
        ) {
            TextButton(
                onClick = { if (canAfford) onPick(tier) },
                enabled = canAfford,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (canAfford) "Redeem" else "Locked",
                    color = if (canAfford) AppColors.Ivory else AppColors.MutedText,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp)
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(req: CashoutRequest) {
    val (statusColor, statusLabel) = when (req.status) {
        "fulfilled" -> AppColors.GoldBright to "FULFILLED"
        "rejected" -> AppColors.EmberSoft to "REJECTED"
        else -> AppColors.Lavender to "PENDING"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Aubergine.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(req.displayLabel, style = MaterialTheme.typography.titleMedium, color = AppColors.Ivory)
            Text(
                "${req.coins} coins · ${formatTs(req.createdAt)}",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.MutedText
            )
            if (req.giftCode.isNotBlank()) {
                Text("Code: ${req.giftCode}", style = MaterialTheme.typography.bodySmall, color = AppColors.GoldBright)
            }
        }
        Surface(
            color = statusColor.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                statusLabel,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp, letterSpacing = 1.sp),
                color = statusColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun ConfirmDialog(
    tier: RewardTier,
    email: String,
    onEmailChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Aubergine,
        titleContentColor = AppColors.GoldBright,
        textContentColor = AppColors.Ivory,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Redeem ${tier.displayLabel}", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 18.sp)) },
        text = {
            Column {
                Text(
                    "You'll spend ${tier.coins} coins. Delivery to your Gmail below within 48 hours.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.IvoryDim
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Delivery email", color = AppColors.Lavender) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppColors.Ivory,
                        unfocusedTextColor = AppColors.Ivory,
                        focusedBorderColor = AppColors.GoldBright,
                        unfocusedBorderColor = AppColors.Lavender,
                        cursorColor = AppColors.GoldBright
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = email.contains("@")) {
                Text("Confirm", color = AppColors.GoldBright, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppColors.Lavender)
            }
        }
    )
}

private val fmt = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())
private fun formatTs(ts: Long): String = if (ts <= 0) "" else fmt.format(Date(ts))

private class CashoutVmFactory(private val uid: String) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
        CashoutViewModel(uid) as T
}

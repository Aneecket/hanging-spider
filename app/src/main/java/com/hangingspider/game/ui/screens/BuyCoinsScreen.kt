package com.hangingspider.game.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hangingspider.game.ui.theme.AppColors
import com.hangingspider.game.ui.theme.AppGradients

data class CoinPack(val id: String, val label: String, val coins: Long, val usd: Double, val badge: String? = null)

private val PACKS = listOf(
    CoinPack("starter", "Starter",   500,   1.00),
    CoinPack("bronze",  "Bronze",   3_000,  5.00, badge = "Popular"),
    CoinPack("silver",  "Silver",   7_000,  10.00),
    CoinPack("gold",    "Gold",    20_000,  25.00, badge = "Best value"),
    CoinPack("royal",   "Royal",   60_000,  60.00)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyCoinsScreen(uid: String) {
    val context = LocalContext.current

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
                            "COIN MARKET",
                            style = MaterialTheme.typography.headlineMedium,
                            color = AppColors.GoldBright
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { inner ->
            LazyColumn(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Text(
                        "Pay once in USDT · coins credited by the oracle",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = AppColors.Lavender,
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                    )
                }
                items(PACKS) { pack -> PackCard(pack) { launchCheckout(context, pack, uid) } }
                item { Spacer(Modifier.height(20.dp)) }
                item { LegalFooter() }
            }
        }
    }
}

@Composable
private fun PackCard(pack: CoinPack, onBuy: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.Aubergine.copy(alpha = 0.75f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(AppColors.GoldBright.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("◉", color = AppColors.GoldBright, fontSize = 26.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                pack.label,
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.Ivory,
                maxLines = 1
            )
            pack.badge?.let {
                Spacer(Modifier.height(2.dp))
                Surface(
                    color = AppColors.Mystic.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        it.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.sp, letterSpacing = 1.sp),
                        color = AppColors.Lavender,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            Text(
                "${pack.coins} coins",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.IvoryDim
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(AppGradients.goldHero)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            TextButton(onClick = onBuy, contentPadding = PaddingValues(0.dp)) {
                Text(
                    "%.2f USDT".format(pack.usd),
                    color = AppColors.PlumDeep,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun LegalFooter() {
    Column {
        Text(
            "How this works",
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.MutedText
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Payment opens a Coinbase Commerce checkout in your browser. When it clears on-chain, our server credits your coins here automatically. Coins are for in-game use only.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.IvoryDim.copy(alpha = 0.6f)
        )
    }
}

private fun launchCheckout(context: android.content.Context, pack: CoinPack, uid: String) {
    // TODO(phase-3-prod): replace with a real Coinbase Commerce charge URL for each pack.
    // Best pattern: create charges server-side (Cloud Function) with metadata { uid, pack: pack.id }
    // and return the hosted URL. The webhook (coinbaseWebhook) credits coins on charge:confirmed.
    val url = "https://commerce.coinbase.com/checkout/REPLACE_WITH_PACK_ID_${pack.id}?uid=$uid"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try { context.startActivity(intent) } catch (_: ActivityNotFoundException) {}
}

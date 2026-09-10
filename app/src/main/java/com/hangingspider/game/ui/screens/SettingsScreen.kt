package com.hangingspider.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hangingspider.game.data.repo.UserRepository
import com.hangingspider.game.ui.theme.AppColors
import com.hangingspider.game.ui.theme.AppGradients
import com.hangingspider.game.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authVm: AuthViewModel,
    onExit: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit
) {
    val authState by authVm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    val syncedEmail = authState.email.takeIf { !authState.isAnonymous && !it.isNullOrBlank() }
    val bannerLabel = syncedEmail ?: "Guest hunter"

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
                            "SETTINGS",
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
            }
        ) { inner ->
            Column(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                AccountBanner(email = bannerLabel)
                Spacer(Modifier.height(24.dp))

                SectionLabel("Sync")
                if (syncedEmail != null) {
                    SettingsRow(
                        title = "Synced with Google",
                        subtitle = syncedEmail,
                        onClick = {}
                    )
                } else {
                    SettingsRow(
                        title = if (authState.linking) "Opening Google…" else "Sync with Google",
                        subtitle = "Save your coins to your Google account so you can play on another device",
                        onClick = { if (!authState.linking) authVm.linkWithGoogle(context) }
                    )
                    authState.linkError?.let {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = AppColors.Signal.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                it,
                                color = AppColors.Rose,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                SectionLabel("Legal")
                SettingsRow(
                    title = "Privacy Policy",
                    subtitle = "What data we collect and why",
                    onClick = onOpenPrivacy
                )
                SettingsRow(
                    title = "Terms of Service",
                    subtitle = "Rules, coins, cash-out policy",
                    onClick = onOpenTerms
                )

                Spacer(Modifier.height(20.dp))
                SectionLabel("Account")
                SettingsRow(
                    title = "Sign out",
                    subtitle = "Keep your data, use another device",
                    onClick = { authVm.signOut(); onExit() }
                )
                SettingsRow(
                    title = "Delete my account",
                    subtitle = "Permanently remove your profile, coins, and history",
                    destructive = true,
                    onClick = { confirmDelete = true }
                )

                status?.let {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        color = AppColors.Signal.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            it,
                            color = AppColors.Rose,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = AppColors.CharcoalMid,
            titleContentColor = AppColors.Signal,
            textContentColor = AppColors.Ivory,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Delete this account?", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp)) },
            text = {
                Column {
                    Text(
                        "This wipes your profile, coin balance, games played, leaderboard rank, in-app messages and cash-out history. It cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.IvoryDim
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Pending cash-outs will be cancelled without refund.",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = AppColors.Rose
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    authVm.deleteAccount { result ->
                        status = when (result) {
                            is UserRepository.DeleteResult.Ok -> null
                            is UserRepository.DeleteResult.NeedsReauth ->
                                "Sign in again in the next 5 minutes to complete deletion."
                            is UserRepository.DeleteResult.Failed ->
                                "Delete failed: ${result.message}"
                        }
                    }
                }) {
                    Text("Delete forever", color = AppColors.Signal, style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Keep account", color = AppColors.Lavender)
                }
            }
        )
    }
}

@Composable
private fun AccountBanner(email: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.CharcoalMid.copy(alpha = 0.75f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(AppColors.Ember.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                (email.firstOrNull()?.uppercaseChar() ?: '?').toString(),
                color = AppColors.GoldBright,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Signed in as", style = MaterialTheme.typography.labelMedium, color = AppColors.MutedText)
            Text(email.ifBlank { "—" }, style = MaterialTheme.typography.titleMedium, color = AppColors.Ivory)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp, letterSpacing = 2.sp),
        color = AppColors.MutedText,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val titleColor = if (destructive) AppColors.Signal else AppColors.Ivory
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.CharcoalMid.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppColors.MutedText)
        }
        Text("›", color = AppColors.MutedText, fontSize = 22.sp)
    }
}


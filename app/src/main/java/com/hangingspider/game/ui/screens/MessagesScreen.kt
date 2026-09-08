package com.hangingspider.game.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hangingspider.game.data.model.AdminMessage
import com.hangingspider.game.ui.theme.AppColors
import com.hangingspider.game.ui.theme.AppGradients
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(vm: MessagesViewModelHolder) {
    val messages by vm.instance.messages.collectAsStateWithLifecycle()

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
                            "THE MISSIVES",
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
            if (messages.isEmpty()) {
                EmptyInbox(modifier = Modifier.padding(inner).fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(inner)
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(messages) { m -> MessageCard(m) }
                }
            }
        }
    }
}

class MessagesViewModelHolder(val instance: com.hangingspider.game.viewmodel.MessagesViewModel)

@Composable
fun rememberMessagesHolder(uid: String): MessagesViewModelHolder {
    val vm: com.hangingspider.game.viewmodel.MessagesViewModel = viewModel(
        key = "msg-$uid",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                com.hangingspider.game.viewmodel.MessagesViewModel(uid) as T
        }
    )
    return remember { MessagesViewModelHolder(vm) }
}

@Composable
private fun EmptyInbox(modifier: Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(AppColors.Aubergine, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("✉", style = MaterialTheme.typography.displayMedium, color = AppColors.GoldBright)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "No whispers yet",
            style = MaterialTheme.typography.headlineMedium,
            color = AppColors.Ivory
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "The raven brings admin messages, rewards, and announcements here.",
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = AppColors.Lavender
        )
    }
}

@Composable
private fun MessageCard(m: AdminMessage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(AppColors.Aubergine.copy(alpha = 0.65f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(AppColors.GoldBright.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✧", color = AppColors.GoldBright, fontSize = 16.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    m.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.Ivory
                )
                Text(
                    "${m.sender} · ${formatTs(m.sentAt)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.MutedText
                )
            }
        }
        if (m.body.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                m.body,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.IvoryDim
            )
        }
    }
}

private val fmt = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())
private fun formatTs(ts: Long): String =
    if (ts <= 0) "" else fmt.format(Date(ts))

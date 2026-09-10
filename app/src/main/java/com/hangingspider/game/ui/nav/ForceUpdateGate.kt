package com.hangingspider.game.ui.nav

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hangingspider.game.BuildConfig
import com.hangingspider.game.ui.theme.AppColors

/**
 * Wraps [content]. Reads /config/minVersionCode from Firebase Realtime Database
 * on mount and any time it changes. If the installed versionCode is below the
 * required minimum, [content] is hidden and a non-dismissible dialog forces the
 * user to open Play Store. Failing to read the config is fail-open (content
 * still renders) so a Firebase outage never bricks the app.
 */
@Composable
fun ForceUpdateGate(content: @Composable () -> Unit) {
    var required by remember { mutableStateOf<Long?>(null) }

    DisposableEffect(Unit) {
        val ref = FirebaseDatabase.getInstance().getReference("config/minVersionCode")
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                required = snap.getValue(Long::class.java) ?: 0L
            }
            override fun onCancelled(error: DatabaseError) {
                required = 0L // fail-open
            }
        }
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }

    val min = required
    if (min != null && BuildConfig.VERSION_CODE < min) {
        ForceUpdateDialog()
    } else {
        content()
    }
}

@Composable
private fun ForceUpdateDialog() {
    val context = LocalContext.current
    val pkg = context.packageName

    Dialog(
        onDismissRequest = { /* non-dismissible */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(AppColors.CharcoalMid)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "UPDATE REQUIRED",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 22.sp,
                        letterSpacing = 3.sp
                    ),
                    color = AppColors.Signal
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "A new version of Hanging Spider is available. Please update from Play Store to keep playing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.Ivory,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(22.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(AppColors.Ember, AppColors.Crimson, AppColors.Signal)
                            )
                        )
                ) {
                    TextButton(
                        modifier = Modifier.fillMaxSize(),
                        onClick = {
                            val playIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=$pkg")
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            try {
                                context.startActivity(playIntent)
                            } catch (_: android.content.ActivityNotFoundException) {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    ) {
                        Text(
                            "UPDATE NOW",
                            color = AppColors.Ivory,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 15.sp,
                                letterSpacing = 2.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

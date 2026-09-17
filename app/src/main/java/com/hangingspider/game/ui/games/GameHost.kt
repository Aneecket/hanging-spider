package com.hangingspider.game.ui.games

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hangingspider.game.game.GameType
import com.hangingspider.game.game.Levels
import com.hangingspider.game.game.WordLists
import com.hangingspider.game.ui.theme.AppColors
import com.hangingspider.game.ui.theme.AppGradients
import com.hangingspider.game.viewmodel.CoinViewModel
import kotlinx.coroutines.delay

data class RoundResult(val won: Boolean, val detail: String)

/** Games whose how-to dialog was already shown this app session. */
private val introShown = mutableSetOf<GameType>()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameHost(level: Int, coinVm: CoinViewModel, onExit: () -> Unit) {
    val type = Levels.gameFor(level)
    val context = LocalContext.current
    val unlockedLevel by coinVm.level.collectAsStateWithLifecycle()

    val wordsReady by produceState(WordLists.isLoaded) {
        WordLists.load(context)
        value = true
    }

    var round by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<RoundResult?>(null) }
    var levelAtRoundStart by remember { mutableIntStateOf(unlockedLevel) }
    var showHelp by remember { mutableStateOf(type !in introShown) }

    BackHandler(onBack = onExit)

    val onRoundEnd: (RoundResult) -> Unit = { r ->
        if (result == null) {
            result = r
            coinVm.onGameFinished(r.won)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppGradients.gameBackground)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                type.title.uppercase(),
                                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                                color = AppColors.GoldBright
                            )
                            Text(
                                "LEVEL $level",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp, letterSpacing = 2.sp),
                                color = AppColors.MutedText
                            )
                        }
                    },
                    navigationIcon = {
                        TextButton(onClick = onExit) {
                            Text("‹ Back", color = AppColors.Lavender, style = MaterialTheme.typography.labelLarge)
                        }
                    },
                    actions = {
                        TextButton(onClick = { showHelp = true }) {
                            Text("?", color = AppColors.Lavender, style = MaterialTheme.typography.titleLarge)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { inner ->
            Box(
                Modifier
                    .padding(inner)
                    .fillMaxSize()
            ) {
                if (!wordsReady) {
                    Text(
                        "Loading words…",
                        color = AppColors.MutedText,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    key(round) {
                        // Timed games stay paused while the how-to dialog is open.
                        GameContent(type, coinVm, paused = result != null || showHelp, onRoundEnd = onRoundEnd)
                    }
                }

                result?.let { r ->
                    ResultOverlay(
                        result = r,
                        unlockedLevel = unlockedLevel.takeIf { it > levelAtRoundStart },
                        onReplay = {
                            levelAtRoundStart = unlockedLevel
                            result = null
                            round++
                        },
                        onExit = onExit
                    )
                }
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false; introShown += type },
            containerColor = AppColors.CharcoalMid,
            titleContentColor = AppColors.GoldBright,
            textContentColor = AppColors.Ivory,
            title = { Text("How to play", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 18.sp)) },
            text = {
                Text(
                    type.howToPlay + "\n\nWin a round to earn ${CoinViewModel.WIN_REWARD} points.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false; introShown += type }) {
                    Text("Got it", color = AppColors.GoldBright)
                }
            }
        )
    }
}

@Composable
private fun GameContent(type: GameType, coinVm: CoinViewModel, paused: Boolean, onRoundEnd: (RoundResult) -> Unit) {
    when (type) {
        GameType.HANGMAN -> HangmanGame(coinVm, onRoundEnd)
        GameType.WORD_SEARCH -> WordSearchGame(paused, onRoundEnd)
        GameType.UNSCRAMBLE -> UnscrambleGame(paused, onRoundEnd)
        GameType.FIVE_LETTER -> FiveLetterGame(onRoundEnd)
        GameType.LETTER_GRID -> LetterGridGame(paused, onRoundEnd)
        GameType.HIVE -> HiveGame(onRoundEnd)
        GameType.CROSSWORD -> CrosswordGame(onRoundEnd)
        GameType.GROUPS -> GroupsGame(onRoundEnd)
        GameType.WORD_PATH -> WordPathGame(onRoundEnd)
        GameType.LETTER_TILES -> LetterTilesGame(onRoundEnd)
        GameType.GHOST -> GhostGame(onRoundEnd)
        GameType.REAL_OR_FAKE -> RealOrFakeGame(onRoundEnd)
        GameType.CLUE_MASTER -> ClueMasterGame(onRoundEnd)
    }
}

@Composable
private fun BoxScope.ResultOverlay(
    result: RoundResult,
    unlockedLevel: Int?,
    onReplay: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(AppColors.Coal.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    )
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (result.won) AppColors.Royal else AppColors.CharcoalMid)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (unlockedLevel != null) {
            Text(
                "LEVEL $unlockedLevel UNLOCKED · ${Levels.gameFor(unlockedLevel).title.uppercase()}",
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
                color = AppColors.Signal,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
        }
        Text(
            if (result.won) "V I C T O R Y" else "F A L L E N",
            style = MaterialTheme.typography.displayMedium.copy(fontSize = 24.sp),
            color = if (result.won) AppColors.GoldBright else AppColors.EmberSoft
        )
        Spacer(Modifier.height(6.dp))
        Text(
            result.detail,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.IvoryDim,
            textAlign = TextAlign.Center
        )
        if (result.won) {
            Spacer(Modifier.height(4.dp))
            Text("+${CoinViewModel.WIN_REWARD} points", style = MaterialTheme.typography.labelLarge, color = AppColors.GoldBright)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Play again", onReplay)
            SecondaryButton("Return home", onExit)
        }
    }
}

/** Seconds-left timer. Games can subtract time as a penalty. */
class Countdown(totalSeconds: Int) {
    var secondsLeft by mutableIntStateOf(totalSeconds)
}

@Composable
fun rememberCountdown(totalSeconds: Int, running: Boolean, onTimeUp: () -> Unit): Countdown {
    val countdown = remember { Countdown(totalSeconds) }
    val timeUp by rememberUpdatedState(onTimeUp)
    LaunchedEffect(running) {
        while (running && countdown.secondsLeft > 0) {
            delay(1000)
            countdown.secondsLeft = (countdown.secondsLeft - 1).coerceAtLeast(0)
        }
        if (running && countdown.secondsLeft == 0) timeUp()
    }
    return countdown
}

package com.hangingspider.game.ui.games

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import com.hangingspider.game.ads.LocalAdManager
import com.hangingspider.game.ads.rememberRewardedAd
import com.hangingspider.game.data.model.DailyRecord
import com.hangingspider.game.data.repo.RoundReport
import com.hangingspider.game.game.Achievements
import com.hangingspider.game.game.AppDay
import com.hangingspider.game.game.GameType
import com.hangingspider.game.game.Levels
import com.hangingspider.game.game.Stars
import com.hangingspider.game.game.WordLists
import com.hangingspider.game.ui.theme.AppColors
import com.hangingspider.game.ui.theme.AppGradients
import com.hangingspider.game.viewmodel.CoinViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/** [stars] is the game's own 1-3 rating for a win; the host lowers it for hints and second chances. */
data class RoundResult(val won: Boolean, val detail: String, val stars: Int = 1, val share: String? = null)

sealed interface PlayMode {
    data object Normal : PlayMode
    /** One round of a locked level, paid for with a rewarded ad. */
    data object Trial : PlayMode
    data class Daily(val day: Long) : PlayMode
}

class SecondChanceOffer(val title: String, val reward: String, val onGranted: () -> Unit, val onDeclined: () -> Unit)

/** What a game can ask of the host during a round. */
class GameSession(
    val random: Random,
    private val pausedState: State<Boolean>,
    private val onEnd: (RoundResult) -> Unit,
    private val onHint: (onGranted: () -> Unit) -> Unit,
    private val onSecondChance: (SecondChanceOffer) -> Unit
) {
    /** True while a dialog or the results panel is up; timers stop and input is ignored. */
    val paused: Boolean get() = pausedState.value

    fun end(result: RoundResult) = onEnd(result)

    /** Asks the player to pay for a hint with points or an ad; [onGranted] runs if they do. */
    fun requestHint(onGranted: () -> Unit) = onHint(onGranted)

    /** Offers one ad-funded second chance per round; otherwise [onDeclined] runs straight away. */
    fun offerSecondChance(title: String, reward: String, onGranted: () -> Unit, onDeclined: () -> Unit) =
        onSecondChance(SecondChanceOffer(title, reward, onGranted, onDeclined))
}

private const val PLAY_URL = "https://play.google.com/store/apps/details?id=com.hangingspider.game"

/** Games whose how-to dialog was already shown this app session. */
private val introShown = mutableSetOf<GameType>()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameHost(level: Int, mode: PlayMode, coinVm: CoinViewModel, onExit: () -> Unit) {
    val type = Levels.gameFor(level)
    val context = LocalContext.current
    val ads = LocalAdManager.current
    val showRewarded = rememberRewardedAd()
    val scope = rememberCoroutineScope()
    val coins = coinVm.profile.collectAsStateWithLifecycle().value?.coins ?: 0L
    val dailyRecords by coinVm.daily.collectAsStateWithLifecycle()

    val wordsReady by produceState(WordLists.isLoaded) {
        WordLists.load(context)
        value = true
    }

    var round by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<RoundResult?>(null) }
    var report by remember { mutableStateOf<RoundReport?>(null) }
    var reward by remember { mutableLongStateOf(0L) }
    var doubled by remember { mutableStateOf(false) }
    var hintsUsed by remember { mutableIntStateOf(0) }
    var secondChanceUsed by remember { mutableStateOf(false) }
    var pendingOffer by remember { mutableStateOf<SecondChanceOffer?>(null) }
    var pendingHint by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showHelp by remember { mutableStateOf(mode !is PlayMode.Daily && type !in introShown) }
    val finishedDaily = (mode as? PlayMode.Daily)?.let { dailyRecords[type.name] }
    val showingOldDaily = finishedDaily != null && result == null && round == 0

    val paused = remember { derivedStateOf { result != null || showHelp || pendingOffer != null || pendingHint != null } }

    BackHandler(onBack = onExit)

    fun nextRound() {
        result = null
        report = null
        doubled = false
        hintsUsed = 0
        secondChanceUsed = false
        round++
    }

    val session = remember(round) {
        val seed = (mode as? PlayMode.Daily)?.let { it.day * 1_000 + type.ordinal } ?: System.nanoTime()
        GameSession(
            random = Random(seed),
            pausedState = paused,
            onEnd = { r ->
                if (result == null) {
                    val stars = if (r.won) Stars.cap(r.stars, hintsUsed, secondChanceUsed) else 0
                    val final = r.copy(stars = stars)
                    result = final
                    reward = coinVm.winReward()
                    scope.launch {
                        report = coinVm.finishRound(level, type, r.won, stars, daily = mode is PlayMode.Daily)
                        if (mode is PlayMode.Daily) {
                            coinVm.saveDaily(type, DailyRecord(r.won, System.currentTimeMillis(), shareText(type, mode, level, final)))
                        }
                    }
                }
            },
            onHint = { granted -> pendingHint = granted },
            onSecondChance = { offer -> if (secondChanceUsed) offer.onDeclined() else pendingOffer = offer }
        )
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
                            val title = (if (mode is PlayMode.Daily) "DAILY · " else "") + type.title.uppercase()
                            Text(
                                title,
                                style = MaterialTheme.typography.headlineMedium.copy(fontSize = if (title.length > 14) 16.sp else 20.sp),
                                color = AppColors.GoldBright,
                                maxLines = 1
                            )
                            Text(
                                when (mode) {
                                    is PlayMode.Daily -> AppDay.label(mode.day).uppercase()
                                    PlayMode.Trial -> "TRIAL ROUND · LEVEL $level"
                                    PlayMode.Normal -> "LEVEL $level"
                                },
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
                when {
                    showingOldDaily -> DailyAlreadyDone(type, finishedDaily!!, onShare = { share(context, finishedDaily.share) }, onExit = onExit)
                    !wordsReady -> Text("Loading words…", color = AppColors.MutedText, modifier = Modifier.align(Alignment.Center))
                    else -> key(round) { GameContent(type, session) }
                }

                result?.let { r ->
                    ResultOverlay(
                        result = r,
                        mode = mode,
                        reward = reward,
                        doubled = doubled,
                        report = report,
                        onDouble = {
                            showRewarded({
                                doubled = true
                                coinVm.addBonus(reward)
                            }, {})
                        },
                        onShare = { share(context, shareText(type, mode, level, r)) },
                        onReplay = {
                            when (mode) {
                                PlayMode.Normal -> {
                                    // Between rounds is the only place an interstitial may appear.
                                    val activity = context as? Activity
                                    if (activity == null) nextRound()
                                    else {
                                        coinVm.stopIdleAccrual()
                                        ads.maybeShowInterstitial(activity) {
                                            coinVm.startIdleAccrual()
                                            nextRound()
                                        }
                                    }
                                }
                                PlayMode.Trial -> showRewarded({ nextRound() }, {})
                                is PlayMode.Daily -> Unit
                            }
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
                    type.howToPlay + "\n\nWin a round to earn ${CoinViewModel.WIN_REWARD} points. " +
                        "Earn up to 3 stars: hints cap a round at 2 stars, and a second chance at 1.",
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

    pendingHint?.let { granted ->
        AlertDialog(
            onDismissRequest = { pendingHint = null },
            containerColor = AppColors.CharcoalMid,
            titleContentColor = AppColors.GoldBright,
            textContentColor = AppColors.Ivory,
            title = { Text("Use a hint?", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 18.sp)) },
            text = {
                Text(
                    "Pay ${CoinViewModel.HINT_COST} points or watch a short ad. Using a hint caps this round at 2 stars.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(
                        enabled = coins >= CoinViewModel.HINT_COST,
                        onClick = {
                            pendingHint = null
                            scope.launch {
                                if (coinVm.spendCoins(CoinViewModel.HINT_COST)) {
                                    hintsUsed++
                                    granted()
                                }
                            }
                        }
                    ) { Text("Use ${CoinViewModel.HINT_COST} points", color = if (coins >= CoinViewModel.HINT_COST) AppColors.GoldBright else AppColors.MutedText) }
                    TextButton(onClick = {
                        pendingHint = null
                        showRewarded({ hintsUsed++; granted() }, {})
                    }) { Text("Watch an ad", color = AppColors.GoldBright) }
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingHint = null }) { Text("Cancel", color = AppColors.Lavender) }
            }
        )
    }

    pendingOffer?.let { offer ->
        AlertDialog(
            onDismissRequest = {},
            containerColor = AppColors.CharcoalMid,
            titleContentColor = AppColors.GoldBright,
            textContentColor = AppColors.Ivory,
            title = { Text(offer.title, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 18.sp)) },
            text = {
                Text(
                    "Watch a short ad for ${offer.reward}. A second chance caps this round at 1 star.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRewarded({
                        secondChanceUsed = true
                        pendingOffer = null
                        offer.onGranted()
                    }, {})
                }) { Text("Watch ad", color = AppColors.GoldBright) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingOffer = null
                    offer.onDeclined()
                }) { Text("End round", color = AppColors.Lavender) }
            }
        )
    }
}

@Composable
private fun GameContent(type: GameType, session: GameSession) {
    when (type) {
        GameType.HANGMAN -> HangmanGame(session)
        GameType.WORD_SEARCH -> WordSearchGame(session)
        GameType.UNSCRAMBLE -> UnscrambleGame(session)
        GameType.FIVE_LETTER -> FiveLetterGame(session)
        GameType.LETTER_GRID -> LetterGridGame(session)
        GameType.HIVE -> HiveGame(session)
        GameType.CROSSWORD -> CrosswordGame(session)
        GameType.GROUPS -> GroupsGame(session)
        GameType.WORD_PATH -> WordPathGame(session)
        GameType.LETTER_TILES -> LetterTilesGame(session)
        GameType.GHOST -> GhostGame(session)
        GameType.REAL_OR_FAKE -> RealOrFakeGame(session)
        GameType.CLUE_MASTER -> ClueMasterGame(session)
    }
}

@Composable
fun StarRow(stars: Int, size: Int = 26) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) {
            Text(
                if (it < stars) "★" else "☆",
                color = if (it < stars) AppColors.GoldBright else AppColors.MutedText,
                fontSize = size.sp
            )
        }
    }
}

@Composable
private fun BoxScope.ResultOverlay(
    result: RoundResult,
    mode: PlayMode,
    reward: Long,
    doubled: Boolean,
    report: RoundReport?,
    onDouble: () -> Unit,
    onShare: () -> Unit,
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
        report?.unlockedLevel?.let { unlocked ->
            Text(
                "LEVEL $unlocked UNLOCKED · ${Levels.gameFor(unlocked).title.uppercase()}",
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
        if (result.won) {
            Spacer(Modifier.height(4.dp))
            StarRow(result.stars)
        }
        Spacer(Modifier.height(6.dp))
        Text(result.detail, style = MaterialTheme.typography.bodyMedium, color = AppColors.IvoryDim, textAlign = TextAlign.Center)
        if (result.won) {
            Spacer(Modifier.height(6.dp))
            Text(
                "+${if (doubled) reward * 2 else reward} points",
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.GoldBright
            )
            if (!doubled) {
                TextButton(onClick = onDouble) {
                    Text("▶ Watch an ad for +$reward more", color = AppColors.GoldBright, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        report?.achievements?.forEach {
            Text(
                "🏆 ${it.title} · +${Achievements.REWARD} points",
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.GoldBright,
                textAlign = TextAlign.Center
            )
        }
        if (mode is PlayMode.Daily) {
            Spacer(Modifier.height(4.dp))
            Text("A new puzzle arrives tomorrow.", style = MaterialTheme.typography.bodySmall, color = AppColors.MutedText)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (mode) {
                PlayMode.Normal -> PrimaryButton("Play again", onReplay)
                PlayMode.Trial -> PrimaryButton("▶ Try again", onReplay)
                is PlayMode.Daily -> Unit
            }
            SecondaryButton("Share", onShare)
            SecondaryButton(if (mode is PlayMode.Daily) "Done" else "Home", onExit)
        }
    }
}

@Composable
private fun DailyAlreadyDone(type: GameType, record: DailyRecord, onShare: () -> Unit, onExit: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (record.won) "Solved today!" else "Today's puzzle is done",
            style = MaterialTheme.typography.headlineMedium,
            color = AppColors.GoldBright
        )
        Spacer(Modifier.height(12.dp))
        Text(
            record.share.lines().drop(1).dropLast(2).joinToString("\n"),
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.Ivory,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text("Come back tomorrow for a new ${type.title} puzzle.", color = AppColors.MutedText, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Share", onShare)
            SecondaryButton("Done", onExit)
        }
    }
}

private fun shareText(type: GameType, mode: PlayMode, level: Int, result: RoundResult): String {
    val header = when (mode) {
        is PlayMode.Daily -> "Hanging Spider · Daily ${type.title} · ${AppDay.label(mode.day)}"
        else -> "Hanging Spider · Level $level ${type.title}"
    }
    val outcome = if (result.won) "★".repeat(result.stars) + "☆".repeat(3 - result.stars) else "Not this time"
    return "$header\n$outcome\n${result.share ?: result.detail}\n\nPlay free: $PLAY_URL"
}

private fun share(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Share your result"))
}

/** Seconds-left timer. Games can add or subtract time. */
class Countdown(totalSeconds: Int) {
    var secondsLeft by mutableIntStateOf(totalSeconds)
    /** Bumped whenever time is added after the timer ran out, so it starts ticking again. */
    var generation by mutableIntStateOf(0)

    fun addTime(seconds: Int) {
        secondsLeft += seconds
        generation++
    }
}

@Composable
fun rememberCountdown(totalSeconds: Int, running: Boolean, onTimeUp: () -> Unit): Countdown {
    val countdown = remember { Countdown(totalSeconds) }
    val timeUp by rememberUpdatedState(onTimeUp)
    LaunchedEffect(running, countdown.generation) {
        while (running && countdown.secondsLeft > 0) {
            delay(1000)
            countdown.secondsLeft = (countdown.secondsLeft - 1).coerceAtLeast(0)
        }
        if (running && countdown.secondsLeft == 0) timeUp()
    }
    return countdown
}

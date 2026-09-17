package com.hangingspider.game.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hangingspider.game.ads.AdManager
import com.hangingspider.game.ads.LocalAdManager
import com.hangingspider.game.game.GameType
import com.hangingspider.game.game.WordLists
import com.hangingspider.game.reminders.EngagementPrefs
import com.hangingspider.game.ui.games.GameHost
import com.hangingspider.game.ui.games.PlayMode
import com.hangingspider.game.ui.screens.AchievementsScreen
import com.hangingspider.game.ui.screens.AuthScreen
import com.hangingspider.game.ui.screens.HomeScreen
import com.hangingspider.game.ui.screens.LeaderboardScreen
import com.hangingspider.game.ui.screens.LegalDoc
import com.hangingspider.game.ui.screens.LegalScreen
import com.hangingspider.game.ui.screens.LevelsScreen
import com.hangingspider.game.ui.screens.MessagesScreen
import com.hangingspider.game.ui.screens.SettingsScreen
import com.hangingspider.game.ui.screens.rememberLeaderboardHolder
import com.hangingspider.game.ui.screens.rememberMessagesHolder
import com.hangingspider.game.viewmodel.AuthViewModel
import com.hangingspider.game.viewmodel.CoinViewModel

object Routes {
    const val AUTH = "auth"
    const val HOME = "home"
    const val GAME = "game/{level}?trial={trial}"
    const val DAILY = "daily/{game}"
    const val LEVELS = "levels"
    const val ACHIEVEMENTS = "achievements"
    const val LEADERBOARD = "leaderboard"
    const val MESSAGES = "messages"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val TERMS = "terms"

    fun game(level: Int, trial: Boolean = false) = "game/$level?trial=$trial"
    fun daily(type: GameType) = "daily/${type.name}"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val authVm: AuthViewModel = viewModel()
    val state by authVm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val adManager = remember { AdManager(context.applicationContext) }

    LaunchedEffect(Unit) {
        adManager.preloadRewarded()
        adManager.preloadInterstitial()
        WordLists.load(context.applicationContext)
    }

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Routes.AUTH
    val showBottomBar = currentRoute in setOf(Routes.HOME, Routes.LEVELS, Routes.LEADERBOARD, Routes.MESSAGES)

    // Hoist CoinViewModel to activity scope so every screen shares one instance
    // (and its live Firebase observer) — otherwise each screen spins up a
    // second VM that hasn't loaded points yet.
    val coinVm: CoinViewModel? = state.uid?.let { uid ->
        viewModel(key = "coin-$uid", factory = CoinVmFactory(uid))
    }
    coinVm?.let {
        BindForegroundAccrual(it)
        MirrorReminderState(it)
    }

    fun exitGame() {
        // Ignore repeat taps while the pop is in flight.
        if (nav.currentBackStackEntry?.destination?.route in setOf(Routes.GAME, Routes.DAILY)) nav.popBackStack()
    }

    CompositionLocalProvider(LocalAdManager provides adManager) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                NavHost(
                    navController = nav,
                    startDestination = if (state.uid != null) Routes.HOME else Routes.AUTH,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(Routes.AUTH) {
                        AuthScreen(authVm) { nav.navigate(Routes.HOME) { popUpTo(Routes.AUTH) { inclusive = true } } }
                    }
                    composable(Routes.HOME) {
                        val vm = coinVm ?: return@composable
                        HomeScreen(
                            coinVm = vm,
                            onPlay = { nav.navigate(Routes.game(vm.level.value)) },
                            onDaily = { type -> nav.navigate(Routes.daily(type)) },
                            onAchievements = { nav.navigate(Routes.ACHIEVEMENTS) },
                            onSettings = { nav.navigate(Routes.SETTINGS) }
                        )
                    }
                    composable(Routes.LEVELS) {
                        val vm = coinVm ?: return@composable
                        LevelsScreen(
                            coinVm = vm,
                            onPlay = { level -> nav.navigate(Routes.game(level)) },
                            onTrial = { level -> nav.navigate(Routes.game(level, trial = true)) },
                            onAchievements = { nav.navigate(Routes.ACHIEVEMENTS) }
                        )
                    }
                    composable(
                        Routes.GAME,
                        arguments = listOf(
                            navArgument("level") { type = NavType.IntType },
                            navArgument("trial") { type = NavType.BoolType; defaultValue = false }
                        )
                    ) { entry ->
                        val vm = coinVm ?: return@composable
                        val unlocked by vm.level.collectAsStateWithLifecycle()
                        val requested = entry.arguments?.getInt("level") ?: 1
                        // Becomes a normal round as soon as the trial level gets unlocked.
                        val trial = entry.arguments?.getBoolean("trial") == true && requested > unlocked
                        GameHost(
                            level = if (entry.arguments?.getBoolean("trial") == true) requested else requested.coerceIn(1, unlocked),
                            mode = if (trial) PlayMode.Trial else PlayMode.Normal,
                            coinVm = vm,
                            onExit = ::exitGame
                        )
                    }
                    composable(Routes.DAILY, arguments = listOf(navArgument("game") { type = NavType.StringType })) { entry ->
                        val vm = coinVm ?: return@composable
                        val type = GameType.entries.firstOrNull { it.name == entry.arguments?.getString("game") } ?: GameType.FIVE_LETTER
                        val today by vm.today.collectAsStateWithLifecycle()
                        GameHost(level = type.ordinal + 1, mode = PlayMode.Daily(today), coinVm = vm, onExit = ::exitGame)
                    }
                    composable(Routes.ACHIEVEMENTS) {
                        val vm = coinVm ?: return@composable
                        AchievementsScreen(coinVm = vm, onExit = { nav.popBackStack() })
                    }
                    composable(Routes.LEADERBOARD) {
                        LeaderboardScreen(vm = rememberLeaderboardHolder())
                    }
                    composable(Routes.MESSAGES) {
                        val uid = state.uid ?: return@composable
                        MessagesScreen(vm = rememberMessagesHolder(uid))
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            authVm = authVm,
                            onExit = { nav.popBackStack() },
                            onOpenPrivacy = { nav.navigate(Routes.PRIVACY) },
                            onOpenTerms = { nav.navigate(Routes.TERMS) }
                        )
                    }
                    composable(Routes.PRIVACY) {
                        LegalScreen(doc = LegalDoc.PRIVACY, onExit = { nav.popBackStack() })
                    }
                    composable(Routes.TERMS) {
                        LegalScreen(doc = LegalDoc.TERMS, onExit = { nav.popBackStack() })
                    }
                }
                if (showBottomBar && state.uid != null) {
                    AppBottomBar(current = currentRoute) { target ->
                        if (target != currentRoute) {
                            nav.navigate(target) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(state.uid) {
        if (state.uid == null) {
            nav.navigate(Routes.AUTH) { popUpTo(0) }
        }
    }
}

@Composable
private fun BindForegroundAccrual(vm: CoinViewModel) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    vm.refreshDay()
                    vm.startIdleAccrual()
                }
                Lifecycle.Event.ON_STOP -> vm.stopIdleAccrual()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
}

/** Keeps the reminder worker's local copy of streak and daily-puzzle state current. */
@Composable
private fun MirrorReminderState(vm: CoinViewModel) {
    val context = LocalContext.current
    val profile by vm.profile.collectAsStateWithLifecycle()
    val daily by vm.daily.collectAsStateWithLifecycle()
    val streakCount = profile?.streakCount ?: 0
    val streakLastDay = profile?.streakLastDay ?: -10
    val dailyDone = daily.size >= 2
    LaunchedEffect(streakCount, streakLastDay, dailyDone) {
        EngagementPrefs(context).mirror(streakCount, streakLastDay, dailyDone)
    }
}

private class CoinVmFactory(private val uid: String) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return CoinViewModel(uid) as T
    }
}

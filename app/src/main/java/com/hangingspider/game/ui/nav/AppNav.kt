package com.hangingspider.game.ui.nav

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hangingspider.game.ads.AdManager
import com.google.firebase.auth.FirebaseAuth
import com.hangingspider.game.ui.screens.AuthScreen
import com.hangingspider.game.ui.screens.BuyCoinsScreen
import com.hangingspider.game.ui.screens.CashoutScreen
import com.hangingspider.game.ui.screens.GameScreen
import com.hangingspider.game.ui.screens.HomeScreen
import com.hangingspider.game.ui.screens.LeaderboardScreen
import com.hangingspider.game.ui.screens.LegalDoc
import com.hangingspider.game.ui.screens.LegalScreen
import com.hangingspider.game.ui.screens.MessagesScreen
import com.hangingspider.game.ui.screens.SettingsScreen
import com.hangingspider.game.ui.screens.rememberLeaderboardHolder
import com.hangingspider.game.ui.screens.rememberMessagesHolder
import com.hangingspider.game.viewmodel.AuthViewModel
import com.hangingspider.game.viewmodel.CoinViewModel

object Routes {
    const val AUTH = "auth"
    const val HOME = "home"
    const val GAME = "game"
    const val LEADERBOARD = "leaderboard"
    const val MESSAGES = "messages"
    const val BUY = "buy"
    const val CASHOUT = "cashout"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val TERMS = "terms"
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
    }

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Routes.AUTH
    val showBottomBar = currentRoute in setOf(Routes.HOME, Routes.LEADERBOARD, Routes.MESSAGES, Routes.BUY)

    // Hoist CoinViewModel to activity scope so every screen shares one instance
    // (and its live Firebase observer) — otherwise CashoutScreen etc. spin up a
    // second VM that hasn't loaded coins yet.
    val coinVm: CoinViewModel? = state.uid?.let { uid ->
        viewModel(key = "coin-$uid", factory = CoinVmFactory(uid))
    }
    coinVm?.let { BindForegroundAccrual(it) }

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
                    authVm = authVm,
                    onPlay = { nav.navigate(Routes.GAME) },
                    onWatchAdForDoubler = {
                        val act = context as? android.app.Activity ?: return@HomeScreen
                        adManager.showRewarded(act) { vm.activateDoubler() }
                    },
                    onCashout = { nav.navigate(Routes.CASHOUT) },
                    onSettings = { nav.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.CASHOUT) {
                val uid = state.uid ?: return@composable
                val vm = coinVm ?: return@composable
                CashoutScreen(
                    uid = uid,
                    email = FirebaseAuth.getInstance().currentUser?.email.orEmpty(),
                    coinVm = vm,
                    onExit = { nav.popBackStack() }
                )
            }
            composable(Routes.GAME) {
                val vm = coinVm ?: return@composable
                GameScreen(
                    coinVm = vm,
                    onExit = {
                        val act = context as? android.app.Activity
                        if (act != null) adManager.showInterstitial(act)
                        nav.popBackStack()
                    }
                )
            }
            composable(Routes.LEADERBOARD) {
                LeaderboardScreen(vm = rememberLeaderboardHolder())
            }
            composable(Routes.MESSAGES) {
                val uid = state.uid ?: return@composable
                MessagesScreen(vm = rememberMessagesHolder(uid))
            }
            composable(Routes.BUY) {
                val uid = state.uid ?: return@composable
                BuyCoinsScreen(uid = uid)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    authVm = authVm,
                    userEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty(),
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
                Lifecycle.Event.ON_START -> vm.startIdleAccrual()
                Lifecycle.Event.ON_STOP -> vm.stopIdleAccrual()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
}

private class CoinVmFactory(private val uid: String) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return CoinViewModel(uid) as T
    }
}

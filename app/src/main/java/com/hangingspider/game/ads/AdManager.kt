package com.hangingspider.game.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.hangingspider.game.BuildConfig

/**
 * Ad unit IDs.
 *
 * Debug builds use Google's public test IDs so ads reliably fill during
 * development. Release builds use the real publisher units.
 *
 * Test IDs: https://developers.google.com/admob/android/test-ads
 */
object AdUnits {
    private const val PROD_REWARDED = "ca-app-pub-2809967663906076/4011511080"
    private const val PROD_INTERSTITIAL = "ca-app-pub-2809967663906076/9504702889"
    /** Empty until a banner unit exists in AdMob; release builds show no banner while empty. */
    private const val PROD_BANNER = ""
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/9214589741"

    val REWARDED: String get() = if (BuildConfig.DEBUG) TEST_REWARDED else PROD_REWARDED
    val INTERSTITIAL: String get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL else PROD_INTERSTITIAL
    val BANNER: String get() = if (BuildConfig.DEBUG) TEST_BANNER else PROD_BANNER
}

enum class RewardOutcome { EARNED, SKIPPED, UNAVAILABLE }

class AdManager(private val context: Context) {

    companion object {
        private const val TAG = "AdManager"
        /** Minimum gap between any two full-screen ads, rewarded ones included. */
        const val INTERSTITIAL_GAP_MS = 150_000L
    }

    private var rewarded: RewardedAd? = null
    private var interstitial: InterstitialAd? = null
    // Starts at app launch so the first interstitial can't appear in the opening minutes.
    private var lastFullScreenAt = SystemClock.elapsedRealtime()

    fun preloadRewarded() {
        if (rewarded != null) return
        RewardedAd.load(context, AdUnits.REWARDED, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewarded = ad }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded load failed: ${error.message}")
                    rewarded = null
                }
            })
    }

    fun preloadInterstitial() {
        if (interstitial != null) return
        InterstitialAd.load(context, AdUnits.INTERSTITIAL, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial load failed: ${error.message}")
                    interstitial = null
                }
            })
    }

    fun showRewarded(activity: Activity, onResult: (RewardOutcome) -> Unit) {
        val ad = rewarded
        if (ad == null) {
            preloadRewarded()
            onResult(RewardOutcome.UNAVAILABLE)
            return
        }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewarded = null
                lastFullScreenAt = SystemClock.elapsedRealtime()
                preloadRewarded()
                onResult(if (earned) RewardOutcome.EARNED else RewardOutcome.SKIPPED)
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewarded = null
                preloadRewarded()
                onResult(RewardOutcome.UNAVAILABLE)
            }
        }
        ad.show(activity) { earned = true }
    }

    /**
     * Shows an interstitial only if one is loaded and at least [INTERSTITIAL_GAP_MS] passed since
     * the last full-screen ad. [onDone] always runs, right away when no ad is shown.
     */
    fun maybeShowInterstitial(activity: Activity, onDone: () -> Unit) {
        val ad = interstitial
        if (ad == null || SystemClock.elapsedRealtime() - lastFullScreenAt < INTERSTITIAL_GAP_MS) {
            if (ad == null) preloadInterstitial()
            onDone()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                lastFullScreenAt = SystemClock.elapsedRealtime()
                preloadInterstitial()
                onDone()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null
                preloadInterstitial()
                onDone()
            }
        }
        ad.show(activity)
    }
}

val LocalAdManager = staticCompositionLocalOf<AdManager> { error("AdManager not provided") }

/**
 * Returns a function that plays a rewarded ad and calls [onEarned] only if the viewer earned the
 * reward. Shows a short message when no ad is ready.
 */
@Composable
fun rememberRewardedAd(): (onEarned: () -> Unit, onNotEarned: () -> Unit) -> Unit {
    val ads = LocalAdManager.current
    val context = LocalContext.current
    return remember(ads, context) {
        { onEarned, onNotEarned ->
            val activity = context as? Activity
            if (activity == null) onNotEarned()
            else ads.showRewarded(activity) { outcome ->
                when (outcome) {
                    RewardOutcome.EARNED -> onEarned()
                    RewardOutcome.SKIPPED -> onNotEarned()
                    RewardOutcome.UNAVAILABLE -> {
                        Toast.makeText(context, "No ad available right now. Try again in a moment.", Toast.LENGTH_SHORT).show()
                        onNotEarned()
                    }
                }
            }
        }
    }
}

/** Adaptive banner for menu screens only; renders nothing when no banner unit is configured. */
@Composable
fun BannerAdSlot(modifier: Modifier = Modifier) {
    val unit = AdUnits.BANNER
    if (unit.isBlank()) return
    val width = LocalConfiguration.current.screenWidthDp
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, width))
                adUnitId = unit
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { it.destroy() }
    )
}

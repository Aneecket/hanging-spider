package com.hangingspider.game.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.hangingspider.game.BuildConfig

/**
 * Ad unit IDs.
 *
 * Debug builds use Google's public test IDs so ads reliably fill during
 * development (production IDs on a fresh AdMob account often return
 * "no fill"). Release builds use the real publisher units.
 *
 * Test IDs: https://developers.google.com/admob/android/test-ads
 */
object AdUnits {
    private const val PROD_REWARDED = "ca-app-pub-5452237321152820/7209378898"
    private const val PROD_INTERSTITIAL = "ca-app-pub-5452237321152820/2723338973"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"

    val REWARDED: String get() = if (BuildConfig.DEBUG) TEST_REWARDED else PROD_REWARDED
    val INTERSTITIAL: String get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL else PROD_INTERSTITIAL
}

class AdManager(private val context: Context) {

    private var rewarded: RewardedAd? = null
    private var interstitial: InterstitialAd? = null

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

    fun showRewarded(activity: Activity, onEarned: () -> Unit) {
        val ad = rewarded
        if (ad == null) {
            preloadRewarded()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { rewarded = null; preloadRewarded() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { rewarded = null }
        }
        ad.show(activity, OnUserEarnedRewardListener { onEarned() })
    }

    /**
     * Shows an interstitial. If no ad is preloaded yet, kicks off a preload
     * for next time and invokes [onDismissed] immediately so the caller can
     * resume paused state (like coin accrual) without waiting for an ad that
     * will never appear.
     */
    fun showInterstitial(activity: Activity, onDismissed: () -> Unit = {}) {
        val ad = interstitial
        if (ad == null) {
            preloadInterstitial()
            onDismissed()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                preloadInterstitial()
                onDismissed()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null
                preloadInterstitial()
                onDismissed()
            }
        }
        ad.show(activity)
    }

    companion object { private const val TAG = "AdManager" }
}

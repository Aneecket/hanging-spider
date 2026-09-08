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

/**
 * Ad unit IDs.
 *
 * Test IDs from https://developers.google.com/admob/android/test-ads — safe to use
 * during development. Replace with your production units before release.
 */
object AdUnits {
    const val REWARDED = "ca-app-pub-5452237321152820/7209378898"
    const val INTERSTITIAL = "ca-app-pub-5452237321152820/2723338973"
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

    fun showInterstitial(activity: Activity) {
        val ad = interstitial ?: return
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { interstitial = null; preloadInterstitial() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { interstitial = null }
        }
        ad.show(activity)
    }

    companion object { private const val TAG = "AdManager" }
}

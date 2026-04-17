package com.ilkokuluncu.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "AdManager"

class AdManager(private val context: Context) {

    // ── SDK hazır mı? ─────────────────────────────────────────────────────────
    private val _sdkReady = MutableStateFlow(false)
    val sdkReady: StateFlow<Boolean> = _sdkReady.asStateFlow()

    // ── Rewarded durum (game-over ekranı butonu için) ─────────────────────────
    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    private var lastInterstitialMs = 0L
    private var gamesCompletedSinceLastAd = 0

    // ── Başlatma ──────────────────────────────────────────────────────────────
    fun initialize() {
        // Çocuk uygulaması: COPPA uyumu — yalnızca çocuğa yönelik, kişisel olmayan reklamlar
        val config = RequestConfiguration.Builder()
            .setTagForChildDirectedTreatment(
                RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
            )
            .setTagForUnderAgeOfConsent(
                RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
            )
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
            .build()
        MobileAds.setRequestConfiguration(config)

        MobileAds.initialize(context) {
            Log.d(TAG, "AdMob SDK hazır")
            _sdkReady.value = true
            loadInterstitial()
            loadRewarded()
        }
    }

    // ── AdRequest (child-directed) ─────────────────────────────────────────────
    private fun buildRequest(): AdRequest = AdRequest.Builder().build()

    // ─────────────────────────── INTERSTITIAL ────────────────────────────────

    fun loadInterstitial() {
        InterstitialAd.load(
            context,
            AdConfig.INTERSTITIAL,
            buildRequest(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial yüklendi")
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial yüklenemedi: ${error.message}")
                    interstitialAd = null
                }
            }
        )
    }

    /**
     * Oyun tamamlandığında çağır.
     * Frekans kontrolü yapar: yeterli süre geçtiyse ve yeterli oyun oynandıysa
     * interstitial gösterir.
     */
    fun onGameCompleted(activity: Activity, onDismissed: () -> Unit = {}) {
        gamesCompletedSinceLastAd++
        val now     = System.currentTimeMillis()
        val elapsed = now - lastInterstitialMs
        val canShow = elapsed >= AdConfig.INTERSTITIAL_MIN_INTERVAL_MS &&
                gamesCompletedSinceLastAd >= AdConfig.INTERSTITIAL_AFTER_N_GAMES

        if (canShow && interstitialAd != null) {
            showInterstitial(activity, onDismissed)
        } else {
            onDismissed()
        }
    }

    private fun showInterstitial(activity: Activity, onDismissed: () -> Unit) {
        val ad = interstitialAd ?: run { onDismissed(); return }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                lastInterstitialMs = System.currentTimeMillis()
                gamesCompletedSinceLastAd = 0
                loadInterstitial()   // bir sonraki için önceden yükle
                onDismissed()
            }
            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                Log.w(TAG, "Interstitial gösterilemedi: ${e.message}")
                interstitialAd = null
                loadInterstitial()
                onDismissed()
            }
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial gösterildi")
            }
        }
        ad.show(activity)
    }

    // ───────────────────────────── REWARDED ──────────────────────────────────

    fun loadRewarded() {
        RewardedAd.load(
            context,
            AdConfig.REWARDED,
            buildRequest(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded yüklendi")
                    rewardedAd = ad
                    _rewardedReady.value = true
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded yüklenemedi: ${error.message}")
                    rewardedAd = null
                    _rewardedReady.value = false
                }
            }
        )
    }

    /**
     * Rewarded reklamı göster. Kullanıcı izlerse [onRewarded] çağrılır.
     * @param onRewarded Ödül verilecek lambda (örn. +1 can)
     */
    fun showRewarded(activity: Activity, onRewarded: () -> Unit, onDismissed: () -> Unit = {}) {
        val ad = rewardedAd ?: run { onDismissed(); return }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _rewardedReady.value = false
                loadRewarded()
                onDismissed()
            }
            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                Log.w(TAG, "Rewarded gösterilemedi: ${e.message}")
                rewardedAd = null
                _rewardedReady.value = false
                loadRewarded()
                onDismissed()
            }
        }
        ad.show(activity) { _ ->
            // RewardItem: type ve amount — biz sabit +1 can veriyoruz
            onRewarded()
        }
    }
}

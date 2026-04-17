package com.ilkokuluncu.app.ads

/**
 * Reklam birimi ID'leri.
 *
 * ⚠️  YAYINLAMADAN ÖNCE:
 *   1. AdMob konsolundan (admob.google.com) uygulama kaydı yap
 *   2. APP_ID değerini AndroidManifest.xml içindeki meta-data ile eşleştir
 *   3. Aşağıdaki TEST_ değerlerini gerçek ad-unit ID'leriyle değiştir
 *      ve TEST_MODE = false yap
 */
object AdConfig {

    // ── Test modunu kapat ve gerçek ID'leri yaz ───────────────────────────────
    const val TEST_MODE = true   // <── false yap + gerçek ID'leri gir

    // ── Gerçek Ad-Unit ID'leri (AdMob konsolundan) ────────────────────────────
    private const val REAL_BANNER_MAIN_MENU      = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
    private const val REAL_BANNER_LEVEL_SELECT   = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
    private const val REAL_INTERSTITIAL          = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
    private const val REAL_REWARDED              = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"

    // ── Google Test Ad-Unit ID'leri (geliştirme/test için) ───────────────────
    private const val TEST_BANNER       = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED     = "ca-app-pub-3940256099942544/5224354917"

    // ── Aktif ID'ler (TEST_MODE'a göre seçilir) ───────────────────────────────
    val BANNER_MAIN_MENU    get() = if (TEST_MODE) TEST_BANNER       else REAL_BANNER_MAIN_MENU
    val BANNER_LEVEL_SELECT get() = if (TEST_MODE) TEST_BANNER       else REAL_BANNER_LEVEL_SELECT
    val INTERSTITIAL        get() = if (TEST_MODE) TEST_INTERSTITIAL else REAL_INTERSTITIAL
    val REWARDED            get() = if (TEST_MODE) TEST_REWARDED     else REAL_REWARDED

    // ── Frekans kapanı ────────────────────────────────────────────────────────
    /** İki interstitial arasındaki minimum süre (ms) */
    const val INTERSTITIAL_MIN_INTERVAL_MS = 3 * 60 * 1000L  // 3 dakika

    /** Kaç oyun tamamlandıktan sonra interstitial gösterilsin */
    const val INTERSTITIAL_AFTER_N_GAMES = 2
}

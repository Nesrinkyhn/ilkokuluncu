package com.ilkokuluncu.app.ui.effects

import android.content.Context
import android.media.MediaPlayer
import com.ilkokuluncu.app.R

/**
 * Arka plan müziğini yönetir.
 *
 * - [startMusic]  : Müziği başlatır (zaten başlatılmışsa tekrar başlatmaz).
 * - [stopMusic]   : Müziği durdurur ve MediaPlayer'ı serbest bırakır.
 * - [applyMute]   : Sessiz / sesli geçişi anlık uygular.
 * - [pauseForLifecycle]  : Activity onPause → müziği durdur.
 * - [resumeForLifecycle] : Activity onResume → sessiz değilse devam et.
 * - [release]     : Activity onDestroy → kaynakları temizle.
 */
class MusicManager(private val context: Context) {

    private var player: MediaPlayer? = null

    /** true = çalması GEREKEN durumdayız (mute bağımsız) */
    private var isActive = false

    // ── Başlat ───────────────────────────────────────────────────────────
    fun startMusic(muted: Boolean) {
        if (isActive) return          // zaten başlatılmış, tekrar çağrılmasın
        isActive = true
        try {
            player = MediaPlayer.create(context, R.raw.background_music)?.apply {
                isLooping = true
                setVolume(0.55f, 0.55f)
                if (!muted) start()
            }
        } catch (_: Exception) { /* kaynak yoksa sessizce atla */ }
    }

    // ── Durdur (saat okuma bölümünden çıkınca) ───────────────────────────
    fun stopMusic() {
        isActive = false
        try { player?.stop() } catch (_: Exception) {}
        player?.release()
        player = null
    }

    // ── Mute toggle → anlık uygula ────────────────────────────────────────
    fun applyMute(muted: Boolean) {
        if (!isActive) return
        try {
            if (muted) player?.pause() else player?.start()
        } catch (_: Exception) {}
    }

    // ── Lifecycle: onPause ────────────────────────────────────────────────
    fun pauseForLifecycle() {
        try { player?.pause() } catch (_: Exception) {}
    }

    // ── Lifecycle: onResume ───────────────────────────────────────────────
    fun resumeForLifecycle(muted: Boolean) {
        if (isActive && !muted) {
            try { player?.start() } catch (_: Exception) {}
        }
    }

    // ── Temizlik ──────────────────────────────────────────────────────────
    fun release() {
        try { player?.release() } catch (_: Exception) {}
        player = null
        isActive = false
    }
}

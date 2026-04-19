package com.ilkokuluncu.app.ui.effects

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.ilkokuluncu.app.R

/**
 * Ekran açıkken background_music.mp3'ü döngüde çalar,
 * ekrandan çıkınca (dispose) otomatik durdurur.
 * Her oyun ekranına yalnızca bir kez eklenmesi yeterlidir.
 */
@Composable
fun GameBackgroundMusic(volume: Float = 0.38f) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val player = try {
            MediaPlayer.create(context, R.raw.background_music)?.apply {
                isLooping = true
                setVolume(volume, volume)
                start()
            }
        } catch (_: Exception) { null }

        onDispose {
            try { player?.stop() } catch (_: Exception) {}
            player?.release()
        }
    }
}

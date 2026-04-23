package com.ilkokuluncu.app.ui.effects

import android.media.MediaPlayer
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ilkokuluncu.app.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Ekran açıkken background_music.mp3'ü döngüde çalar,
 * ekrandan çıkınca (dispose) otomatik durdurur.
 * Her oyun ekranına yalnızca bir kez eklenmesi yeterlidir.
 */
@Composable
fun GameBackgroundMusic(volume: Float = 0.38f) {
    val context = LocalContext.current
    val playerRef = remember<MutableState<MediaPlayer?>> { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        var player: MediaPlayer? = null
        try {
            withContext(Dispatchers.IO) {
                val p = MediaPlayer.create(context, R.raw.background_music)
                    ?: return@withContext
                ensureActive() // Eğer composable dispose edildiyse iptal et
                player = p
                p.isLooping = true
                p.setVolume(volume, volume)
                p.start()
            }
            playerRef.value = player
        } catch (e: CancellationException) {
            // Composable dispose edildi, player'ı hemen serbest bırak
            player?.runCatching { stop() }
            player?.runCatching { release() }
            throw e // CancellationException'ı tekrar fırlat
        } catch (_: Exception) {
            player?.runCatching { release() }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            playerRef.value?.runCatching { stop() }
            playerRef.value?.runCatching { release() }
            playerRef.value = null
        }
    }
}

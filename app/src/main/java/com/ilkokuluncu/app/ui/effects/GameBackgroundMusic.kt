package com.ilkokuluncu.app.ui.effects

import android.media.MediaPlayer
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ilkokuluncu.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Ekran açıkken background_music.mp3'ü döngüde çalar,
 * ekrandan çıkınca (dispose) otomatik durdurur.
 * Her oyun ekranına yalnızca bir kez eklenmesi yeterlidir.
 */
@Composable
fun GameBackgroundMusic(volume: Float = 0.38f) {
    val context = LocalContext.current
    val playerRef = remember<MutableState<MediaPlayer?>> { mutableStateOf(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.Default) {
            try {
                val player = MediaPlayer.create(context, R.raw.background_music)
                if (player != null) {
                    player.isLooping = true
                    player.setVolume(volume, volume)
                    player.start()
                    playerRef.value = player
                }
            } catch (_: Exception) {
                playerRef.value = null
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                playerRef.value?.stop()
            } catch (_: Exception) {}
            try {
                playerRef.value?.release()
            } catch (_: Exception) {}
            playerRef.value = null
        }
    }
}

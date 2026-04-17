
package com.ilkokuluncu.app.ui.effects

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ilkokuluncu.app.R

class SoundEffectPlayer(private val context: Context) {
    private val activePlayers = mutableListOf<MediaPlayer>()

    fun playTap() = play(R.raw.clock_tap, volume = 0.65f)

    fun playCorrect() = play(R.raw.clock_correct, volume = 0.85f)

    fun playWrongWithVibration() {
        play(R.raw.clock_wrong, volume = 0.9f)
        vibrateWrong()
    }

    private fun play(@RawRes resId: Int, volume: Float) {
        val player = MediaPlayer.create(context, resId) ?: return
        player.setVolume(volume, volume)
        activePlayers += player
        player.setOnCompletionListener {
            it.release()
            activePlayers.remove(it)        }
        player.start()
    }

    private fun vibrateWrong() {
        val vibrator: Vibrator? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

        vibrator ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 60, 40, 120),
                    intArrayOf(0, 90, 0, 180),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(180)
        }
    }

    fun release() {
        activePlayers.forEach {
            runCatching { it.stop() }
            it.release()
        }
        activePlayers.clear()
    }
}

@Composable
fun rememberSoundEffectPlayer(): SoundEffectPlayer {
    val context = LocalContext.current
    val player = remember(context) { SoundEffectPlayer(context) }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    return player
}

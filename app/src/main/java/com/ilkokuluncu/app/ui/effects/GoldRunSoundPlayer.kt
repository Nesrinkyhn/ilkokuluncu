package com.ilkokuluncu.app.ui.effects

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RawRes
import com.ilkokuluncu.app.R
import com.ilkokuluncu.app.data.GoldRunSound

class GoldRunSoundPlayer(private val context: Context) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val idCorrect = pool.load(context, R.raw.clock_correct, 1) // coin correct
    private val idWrong   = pool.load(context, R.raw.clock_wrong,   1) // coin wrong / thorn
    private val idTap     = pool.load(context, R.raw.clock_tap,     1) // pit fall / game over

    private var bgMusic: MediaPlayer? = null

    fun startMusic() {
        if (bgMusic != null) return
        try {
            bgMusic = MediaPlayer.create(context, R.raw.background_music)?.apply {
                isLooping = true
                setVolume(0.40f, 0.40f)
                start()
            }
        } catch (_: Exception) {}
    }

    fun stopMusic() {
        try { bgMusic?.stop() } catch (_: Exception) {}
        bgMusic?.release()
        bgMusic = null
    }

    fun pauseMusic() { try { bgMusic?.pause() } catch (_: Exception) {} }
    fun resumeMusic() { try { bgMusic?.start() } catch (_: Exception) {} }

    fun play(sound: GoldRunSound) {
        when (sound) {
            GoldRunSound.CoinCorrect -> {
                pool.play(idCorrect, 0.9f, 0.9f, 1, 0, 1.0f)
            }
            GoldRunSound.CoinNormal -> {
                pool.play(idTap, 0.7f, 0.7f, 1, 0, 1.3f)
            }
            GoldRunSound.CoinWrong -> {
                pool.play(idWrong, 0.85f, 0.85f, 1, 0, 1.0f)
                vibrate(longArrayOf(0, 40, 20, 80), intArrayOf(0, 100, 0, 160))
            }
            GoldRunSound.ThornHit -> {
                pool.play(idWrong, 0.75f, 0.75f, 1, 0, 0.85f)
                vibrate(longArrayOf(0, 30, 20, 60), intArrayOf(0, 80, 0, 130))
            }
            GoldRunSound.PitFall -> {
                pool.play(idTap, 0.9f, 0.9f, 1, 0, 0.7f)
                vibrate(longArrayOf(0, 80, 30, 120), intArrayOf(0, 120, 0, 200))
            }
            GoldRunSound.Victory -> {
                pool.play(idCorrect, 1.0f, 1.0f, 1, 0, 1.2f)
            }
            GoldRunSound.GameOver -> {
                pool.play(idWrong, 1.0f, 1.0f, 1, 0, 0.75f)
                vibrate(longArrayOf(0, 100, 50, 200), intArrayOf(0, 150, 0, 255))
            }
        }
    }

    private fun vibrate(timings: LongArray, amps: IntArray) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings.fold(0L) { acc, v -> acc + v })
        }
    }

    fun release() {
        stopMusic()
        pool.release()
    }
}

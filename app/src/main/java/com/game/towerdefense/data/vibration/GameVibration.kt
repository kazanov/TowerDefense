package com.game.towerdefense.data.vibration

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class GameVibration(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    @Volatile
    var enabled: Boolean = true

    private var lastTime = 0L

    fun vibrate(durationMs: Long) {
        if (!enabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val now = SystemClock.uptimeMillis()
        if (now - lastTime < MIN_GAP_MS) return
        lastTime = now
        try {
            v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {
            // Некоторые прошивки бросают SecurityException — игра продолжает работать.
        }
    }

    private companion object {
        const val MIN_GAP_MS = 100L
    }
}

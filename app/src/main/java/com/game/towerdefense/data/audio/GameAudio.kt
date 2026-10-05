package com.game.towerdefense.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.io.File

/** Звуковые эффекты (SoundPool) и фоновая музыка (MediaPlayer). Все публичные методы — с главного потока. */
class GameAudio(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(12)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds = IntArray(Sfx.entries.size)
    private val lastPlay = LongArray(Sfx.entries.size)

    private var music: MediaPlayer? = null
    @Volatile private var soundEnabled = true
    private var musicEnabled = true
    private var foreground = false
    private var initialized = false

    fun init() {
        if (initialized) return
        initialized = true
        Thread({
            try {
                val dir = File(appContext.cacheDir, "audio").apply { mkdirs() }
                for (sfx in Sfx.entries) {
                    val file = File(dir, "${sfx.name.lowercase()}_$VERSION.wav")
                    if (!file.exists()) SoundSynth.writeWav(file, SoundSynth.generate(sfx))
                    soundIds[sfx.ordinal] = soundPool.load(file.absolutePath, 1)
                }
                val musicFile = File(dir, "music_$VERSION.wav")
                if (!musicFile.exists()) SoundSynth.writeWav(musicFile, SoundSynth.music())
                mainHandler.post { createMusic(musicFile) }
            } catch (t: Throwable) {
                Log.e(TAG, "Audio init failed", t)
            }
        }, "audio-init").start()
    }

    private fun createMusic(file: File) {
        try {
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            player.setDataSource(file.absolutePath)
            player.isLooping = true
            player.setVolume(0.45f, 0.45f)
            player.prepare()
            music = player
            updateMusic()
        } catch (t: Throwable) {
            Log.e(TAG, "Music init failed", t)
        }
    }

    fun play(sfx: Sfx) {
        if (!soundEnabled) return
        val id = soundIds[sfx.ordinal]
        if (id == 0) return
        val now = SystemClock.uptimeMillis()
        if (now - lastPlay[sfx.ordinal] < sfx.minGapMs) return
        lastPlay[sfx.ordinal] = now
        soundPool.play(id, sfx.volume, sfx.volume, 1, 0, 1f)
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }

    fun setMusicEnabled(enabled: Boolean) {
        musicEnabled = enabled
        updateMusic()
    }

    fun onForeground(isForeground: Boolean) {
        foreground = isForeground
        updateMusic()
    }

    private fun updateMusic() {
        val player = music ?: return
        try {
            if (musicEnabled && foreground) {
                if (!player.isPlaying) player.start()
            } else if (player.isPlaying) {
                player.pause()
            }
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Music state error", e)
        }
    }

    private companion object {
        const val TAG = "GameAudio"
        const val VERSION = "v1"
    }
}

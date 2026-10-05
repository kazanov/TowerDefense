package com.game.towerdefense.data.audio

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/**
 * Процедурный синтез звуков и музыки — игра не требует аудиофайлов в assets.
 * Звуки генерируются один раз и кэшируются в cacheDir как WAV.
 */
object SoundSynth {
    const val RATE = 22050

    enum class Wave { SINE, SQUARE, TRIANGLE, NOISE }

    private fun osc(wave: Wave, phase: Double, rnd: Random): Float {
        val ph = phase - floor(phase)
        return when (wave) {
            Wave.SINE -> sin(2.0 * PI * ph).toFloat()
            Wave.SQUARE -> if (ph < 0.5) 0.8f else -0.8f
            Wave.TRIANGLE -> (4.0 * abs(ph - 0.5) - 1.0).toFloat()
            Wave.NOISE -> rnd.nextFloat() * 2f - 1f
        }
    }

    /**
     * @param decay показатель затухания огибающей (больше — быстрее затухает)
     * @param lowPass коэффициент однополюсного ФНЧ (1 = без фильтра)
     */
    fun tone(
        duration: Float,
        fromHz: Float,
        toHz: Float,
        wave: Wave,
        volume: Float,
        decay: Float = 2f,
        lowPass: Float = 1f,
    ): FloatArray {
        val n = max(1, (duration * RATE).toInt())
        val out = FloatArray(n)
        val rnd = Random(1234)
        val attack = max(1, (0.004f * RATE).toInt())
        var phase = 0.0
        var lp = 0f
        for (i in 0 until n) {
            val t = i.toFloat() / n
            val hz = fromHz + (toHz - fromHz) * t
            phase += hz / RATE
            val raw = osc(wave, phase, rnd)
            lp += lowPass * (raw - lp)
            val env = (if (i < attack) i.toFloat() / attack else 1f) * (1f - t).pow(decay)
            out[i] = lp * env * volume
        }
        return out
    }

    private fun note(midiNote: Int, duration: Float, wave: Wave, volume: Float, decay: Float = 1.5f): FloatArray {
        val hz = midi(midiNote)
        return tone(duration, hz, hz, wave, volume, decay)
    }

    fun midi(note: Int): Float = (440.0 * 2.0.pow((note - 69) / 12.0)).toFloat()

    fun concat(vararg parts: FloatArray): FloatArray {
        val out = FloatArray(parts.sumOf { it.size })
        var offset = 0
        for (p in parts) {
            p.copyInto(out, offset)
            offset += p.size
        }
        return out
    }

    fun mix(a: FloatArray, b: FloatArray): FloatArray {
        val out = FloatArray(max(a.size, b.size))
        for (i in a.indices) out[i] += a[i]
        for (i in b.indices) out[i] += b[i]
        return out
    }

    fun toPcm(data: FloatArray): ShortArray =
        ShortArray(data.size) { (data[it].coerceIn(-1f, 1f) * 32000f).toInt().toShort() }

    fun generate(sfx: Sfx): ShortArray = toPcm(
        when (sfx) {
            Sfx.CLICK -> tone(0.04f, 900f, 900f, Wave.SQUARE, 0.25f)
            Sfx.BUILD -> concat(
                tone(0.07f, 300f, 500f, Wave.SQUARE, 0.35f, 0.5f, 0.4f),
                tone(0.13f, 500f, 850f, Wave.SQUARE, 0.35f, 1.5f, 0.4f),
            )
            Sfx.UPGRADE -> concat(
                note(72, 0.07f, Wave.SQUARE, 0.3f, 0.5f),
                note(76, 0.07f, Wave.SQUARE, 0.3f, 0.5f),
                note(79, 0.18f, Wave.SQUARE, 0.3f, 1.5f),
            )
            Sfx.SELL -> tone(0.18f, 900f, 300f, Wave.TRIANGLE, 0.5f)
            Sfx.ARROW -> mix(
                tone(0.08f, 1400f, 500f, Wave.TRIANGLE, 0.35f, 3f),
                tone(0.05f, 0f, 0f, Wave.NOISE, 0.2f, 3f, 0.5f),
            )
            Sfx.CANNON -> mix(
                tone(0.25f, 140f, 45f, Wave.SINE, 0.8f, 2f),
                tone(0.2f, 0f, 0f, Wave.NOISE, 0.6f, 3f, 0.15f),
            )
            Sfx.ICE -> mix(
                tone(0.14f, 1800f, 2600f, Wave.SINE, 0.3f, 2f),
                tone(0.1f, 2600f, 3200f, Wave.SINE, 0.12f, 2f),
            )
            Sfx.MAGIC -> mix(
                tone(0.2f, 500f, 1300f, Wave.TRIANGLE, 0.35f, 1.5f),
                tone(0.2f, 750f, 1950f, Wave.SINE, 0.15f, 1.5f),
            )
            Sfx.HIT -> tone(0.05f, 0f, 0f, Wave.NOISE, 0.35f, 2f, 0.4f)
            Sfx.EXPLOSION -> mix(
                tone(0.5f, 0f, 0f, Wave.NOISE, 1.0f, 2.5f, 0.08f),
                tone(0.35f, 90f, 35f, Wave.SINE, 0.7f, 2f),
            )
            Sfx.DEATH -> tone(0.16f, 520f, 140f, Wave.SQUARE, 0.22f, 1.5f, 0.5f)
            Sfx.WAVE_START -> concat(
                note(67, 0.14f, Wave.SQUARE, 0.3f, 0.5f),
                note(72, 0.32f, Wave.SQUARE, 0.3f, 1.2f),
            )
            Sfx.VICTORY -> concat(
                note(72, 0.12f, Wave.SQUARE, 0.3f, 0.6f),
                note(76, 0.12f, Wave.SQUARE, 0.3f, 0.6f),
                note(79, 0.12f, Wave.SQUARE, 0.3f, 0.6f),
                note(84, 0.6f, Wave.SQUARE, 0.3f, 1.5f),
            )
            Sfx.DEFEAT -> concat(
                note(67, 0.25f, Wave.TRIANGLE, 0.5f, 0.6f),
                note(63, 0.25f, Wave.TRIANGLE, 0.5f, 0.6f),
                note(60, 0.7f, Wave.TRIANGLE, 0.5f, 1.5f),
            )
            Sfx.BASE_HIT -> mix(
                tone(0.3f, 160f, 50f, Wave.SQUARE, 0.35f, 2f, 0.3f),
                tone(0.2f, 0f, 0f, Wave.NOISE, 0.35f, 3f, 0.2f),
            )
        }
    )

    /** Небольшая зацикленная мелодия в ля миноре (~9 секунд). */
    fun music(): ShortArray {
        val bpm = 108f
        val eighth = 60f / bpm / 2f
        val melody = intArrayOf(
            69, 72, 76, 72, 74, 72, 71, 67,
            69, 72, 76, 79, 77, 76, 74, 0,
            72, 74, 76, 77, 76, 74, 72, 71,
            69, 71, 72, 71, 69, 0, 64, 0,
        )
        val bass = intArrayOf(45, 52, 45, 52, 41, 48, 41, 48, 48, 55, 48, 55, 40, 47, 44, 47)
        val total = (melody.size * eighth * RATE).toInt()
        val out = FloatArray(total)

        fun place(start: Float, data: FloatArray) {
            val s0 = (start * RATE).toInt()
            for (i in data.indices) {
                val j = s0 + i
                if (j < total) out[j] += data[i]
            }
        }

        melody.forEachIndexed { i, n ->
            if (n > 0) place(i * eighth, note(n, eighth * 0.95f, Wave.TRIANGLE, 0.22f, 1.2f))
        }
        bass.forEachIndexed { i, n ->
            val hz = midi(n)
            place(i * eighth * 2f, tone(eighth * 1.9f, hz, hz, Wave.SQUARE, 0.12f, 0.8f, 0.15f))
        }
        for (i in melody.indices) {
            place(i * eighth, tone(0.03f, 0f, 0f, Wave.NOISE, if (i % 2 == 0) 0.06f else 0.035f, 3f, 0.9f))
        }
        return toPcm(out)
    }

    fun writeWav(file: File, samples: ShortArray) {
        val dataSize = samples.size * 2
        val buf = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray(Charsets.US_ASCII))
        buf.putInt(36 + dataSize)
        buf.put("WAVE".toByteArray(Charsets.US_ASCII))
        buf.put("fmt ".toByteArray(Charsets.US_ASCII))
        buf.putInt(16)
        buf.putShort(1.toShort()) // PCM
        buf.putShort(1.toShort()) // mono
        buf.putInt(RATE)
        buf.putInt(RATE * 2)
        buf.putShort(2.toShort())
        buf.putShort(16.toShort())
        buf.put("data".toByteArray(Charsets.US_ASCII))
        buf.putInt(dataSize)
        for (s in samples) buf.putShort(s)
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeBytes(buf.array())
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }
}

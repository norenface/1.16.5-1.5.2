package com.iidx.controller

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.*
import kotlin.random.Random

/**
 * Generates and streams a vinyl-scratch audio effect in real time.
 *
 * The turntable axis delta drives a virtual read-head through a pre-generated
 * "vinyl noise" loop: positive delta = forward playback, negative = reverse,
 * larger magnitude = faster/louder scratch.
 */
class ScratchEngine {

    private val sampleRate = 44100
    private val loopSamples = sampleRate * 2          // 2-second loop buffer
    private val chunkSize   = 512                      // samples per write

    // Pre-generated vinyl-noise texture (filtered white noise)
    private val vinylLoop: ShortArray = buildVinylLoop()

    // Atomic speed value updated from the UI thread
    @Volatile private var targetSpeed = 0f
    @Volatile private var lastSpeedUpdateMs = 0L

    @Volatile private var running = true

    private var readHeadF = 0.0   // fractional position in loop (supports sub-sample)

    private val audioTrack: AudioTrack

    init {
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(chunkSize * 4)

        audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            minBuf * 2,
            AudioTrack.MODE_STREAM,
            android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        audioTrack.play()
        startStreamThread()
    }

    /**
     * Called from the UI/input thread with the normalised turntable axis delta.
     * [delta] is the change in axis value per motion-event frame (range ≈ −1 .. +1).
     */
    fun setSpeed(delta: Float) {
        targetSpeed = (delta * 6f).coerceIn(-1f, 1f)
        lastSpeedUpdateMs = System.currentTimeMillis()
    }

    // ---- private ----

    private fun buildVinylLoop(): ShortArray {
        val buf = ShortArray(loopSamples)
        val rng = Random(31337)

        // Two-pole low-pass filtered white noise (vinyl character)
        var lp1 = 0f
        var lp2 = 0f

        for (i in 0 until loopSamples) {
            val noise = rng.nextFloat() * 2f - 1f
            lp1 = lp1 * 0.82f + noise * 0.18f
            lp2 = lp2 * 0.82f + lp1  * 0.18f

            // Mild waveshaping for grit
            val shaped = tanh(lp2 * 2.0f) * 0.5f
            buf[i] = (shaped * 18000f).toInt().coerceIn(
                Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()
            ).toShort()
        }
        return buf
    }

    private fun startStreamThread() {
        val t = Thread {
            val chunk = ShortArray(chunkSize)
            var currentSpeed = 0f

            while (running) {
                val now = System.currentTimeMillis()

                // Decay speed if no update in 80ms (turntable slowing down)
                if (now - lastSpeedUpdateMs > 80L) {
                    targetSpeed *= 0.75f
                    if (abs(targetSpeed) < 0.005f) targetSpeed = 0f
                }

                // Smooth towards target speed
                currentSpeed = currentSpeed * 0.80f + targetSpeed * 0.20f
                val absSpd = abs(currentSpeed)
                val volume = (absSpd * 3f).coerceIn(0f, 1f)

                if (absSpd > 0.005f) {
                    // How many source samples to advance per output sample
                    val stepPerSample = absSpd * 4.0 + 0.2
                    val forward = currentSpeed > 0

                    for (i in 0 until chunkSize) {
                        if (forward) {
                            readHeadF += stepPerSample
                            if (readHeadF >= loopSamples) readHeadF -= loopSamples
                        } else {
                            readHeadF -= stepPerSample
                            if (readHeadF < 0) readHeadF += loopSamples
                        }

                        // Linear interpolation between adjacent samples
                        val idx0 = readHeadF.toInt() % loopSamples
                        val idx1 = (idx0 + 1) % loopSamples
                        val frac = readHeadF - floor(readHeadF)
                        val s = vinylLoop[idx0] * (1.0 - frac) + vinylLoop[idx1] * frac

                        chunk[i] = (s * volume).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                            .toShort()
                    }
                } else {
                    chunk.fill(0)
                }

                audioTrack.write(chunk, 0, chunkSize)
            }
        }
        t.isDaemon = true
        t.priority = Thread.MAX_PRIORITY
        t.name = "ScratchEngine"
        t.start()
    }

    fun release() {
        running = false
        audioTrack.stop()
        audioTrack.release()
    }

    private fun tanh(x: Float): Float {
        val e = exp(x.toDouble() * 2.0)
        return ((e - 1.0) / (e + 1.0)).toFloat()
    }
}

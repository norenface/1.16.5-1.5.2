package com.iidx.controller

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.*

/**
 * Synthesizes piano-like tones for the 7 IIDX buttons.
 * Each button maps to a note in a minor pentatonic scale tuned for DJ music.
 */
class AudioEngine {

    private val sampleRate = 44100
    private val executor = Executors.newFixedThreadPool(4)

    // Notes for keys 1-7: A minor pentatonic + octave
    // White keys: A3, C4, D4, E4, A4 mapped to positions 0,2,4,6
    // Black keys: C4, Eb4, G4 mapped to positions 1,3,5
    private val noteFrequencies = floatArrayOf(
        220.00f,  // Key 1 (white) A3
        261.63f,  // Key 2 (black) C4
        293.66f,  // Key 3 (white) D4
        311.13f,  // Key 4 (black) Eb4
        329.63f,  // Key 5 (white) E4
        392.00f,  // Key 6 (black) G4
        440.00f   // Key 7 (white) A4
    )

    private val noteBuffers: Array<ShortArray> = Array(7) { i ->
        synthesizeNote(noteFrequencies[i], durationMs = 1200)
    }

    /**
     * Synthesizes a piano-like tone with ADSR envelope and harmonics.
     */
    private fun synthesizeNote(freq: Float, durationMs: Int): ShortArray {
        val numSamples = sampleRate * durationMs / 1000
        val buf = ShortArray(numSamples)

        val attackSamples  = (sampleRate * 0.008).toInt()  // 8ms attack
        val decaySamples   = (sampleRate * 0.12).toInt()   // 120ms decay
        val sustainLevel   = 0.55
        val releaseSamples = (sampleRate * 0.35).toInt()   // 350ms release

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate

            val env = when {
                i < attackSamples ->
                    i.toDouble() / attackSamples
                i < attackSamples + decaySamples ->
                    1.0 - (1.0 - sustainLevel) * (i - attackSamples).toDouble() / decaySamples
                i < numSamples - releaseSamples ->
                    sustainLevel
                else ->
                    sustainLevel * (numSamples - i).toDouble() / releaseSamples
            }.coerceIn(0.0, 1.0)

            // Fundamental + harmonics (piano timbre)
            val f1 = sin(2.0 * PI * freq      * t)
            val f2 = sin(2.0 * PI * freq * 2  * t) * 0.45
            val f3 = sin(2.0 * PI * freq * 3  * t) * 0.20
            val f4 = sin(2.0 * PI * freq * 4  * t) * 0.10
            val f5 = sin(2.0 * PI * freq * 6  * t) * 0.04

            val sample = env * (f1 + f2 + f3 + f4 + f5) / 1.79
            buf[i] = (sample * 28000).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return buf
    }

    /**
     * Plays the note for button [index] (0-6) on a background thread.
     */
    fun playButton(index: Int) {
        if (index !in 0..6) return
        val buffer = noteBuffers[index]

        executor.submit {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val track = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
                maxOf(buffer.size * 2, minBuf),
                AudioTrack.MODE_STATIC,
                android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            track.write(buffer, 0, buffer.size)
            track.play()
            // Playback completes; release on notification listener
            track.setPlaybackPositionUpdateListener(object :
                AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack) {
                    t.release()
                }
                override fun onPeriodicNotification(t: AudioTrack) {}
            })
            track.notificationMarkerPosition = buffer.size - 1
        }
    }

    fun release() {
        executor.shutdown()
    }
}

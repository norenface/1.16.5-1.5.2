package com.iidx.controller

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.*

/**
 * Synthesizes piano-like tones for 7 IIDX buttons.
 * Uses pre-created static AudioTracks with reloadStaticData() for replaying.
 */
class AudioEngine {

    private val sampleRate = 44100

    // A minor pentatonic scale across 7 keys
    private val noteFrequencies = floatArrayOf(
        220.00f,  // Key 1 (white) A3
        261.63f,  // Key 2 (black) C4
        293.66f,  // Key 3 (white) D4
        311.13f,  // Key 4 (black) Eb4
        329.63f,  // Key 5 (white) E4
        392.00f,  // Key 6 (black) G4
        440.00f   // Key 7 (white) A4
    )

    private val tracks = arrayOfNulls<AudioTrack>(7)

    init {
        for (i in 0..6) {
            try {
                val buffer = synthesizeNote(noteFrequencies[i])
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(buffer, 0, buffer.size)
                tracks[i] = track
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    private fun synthesizeNote(freq: Float, durationMs: Int = 1200): ShortArray {
        val n = sampleRate * durationMs / 1000
        val buf = ShortArray(n)
        val attack  = (sampleRate * 0.008).toInt()
        val decay   = (sampleRate * 0.12).toInt()
        val sustain = 0.55
        val release = (sampleRate * 0.35).toInt()

        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val env = when {
                i < attack               -> i.toDouble() / attack
                i < attack + decay       -> 1.0 - (1.0 - sustain) * (i - attack).toDouble() / decay
                i < n - release          -> sustain
                else                     -> sustain * (n - i).toDouble() / release
            }.coerceIn(0.0, 1.0)

            val wave = sin(2.0 * PI * freq * t) +
                       sin(2.0 * PI * freq * 2 * t) * 0.45 +
                       sin(2.0 * PI * freq * 3 * t) * 0.20 +
                       sin(2.0 * PI * freq * 4 * t) * 0.10

            buf[i] = (env * wave / 1.75 * 28000)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return buf
    }

    fun playButton(index: Int) {
        if (index !in 0..6) return
        try {
            val track = tracks[index] ?: return
            // Stop any current playback, rewind, and play again
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop()
            }
            track.reloadStaticData()
            track.play()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun release() {
        for (track in tracks) {
            try { track?.stop(); track?.release() } catch (_: Throwable) {}
        }
    }
}

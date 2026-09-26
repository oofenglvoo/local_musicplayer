package com.localmusic.player.playback

import android.media.MediaMetadataRetriever
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

enum class ReplayGainMode(
    val key: String,
    @androidx.annotation.StringRes val labelRes: Int,
    val preampDb: Float,
) {
    OFF("off", com.localmusic.player.R.string.replay_gain_off, 0f),
    TRACK("track", com.localmusic.player.R.string.replay_gain_track, 0f),
    ALBUM("album", com.localmusic.player.R.string.replay_gain_album, 0f),
    TRACK_PREVENT_CLIP("track_clip", com.localmusic.player.R.string.replay_gain_track_clip, -2f),
    ALBUM_PREVENT_CLIP("album_clip", com.localmusic.player.R.string.replay_gain_album_clip, -2f);

    companion object {
        fun from(key: String?): ReplayGainMode =
            entries.firstOrNull { it.key == key } ?: OFF
    }
}

data class ReplayGainInfo(
    val trackGainDb: Float?,
    val albumGainDb: Float?,
    val trackPeak: Float?,
    val albumPeak: Float?,
)

object ReplayGainReader {

    private const val RG_TRACK_GAIN = 0
    private const val RG_TRACK_PEAK = 1
    private const val RG_ALBUM_GAIN = 2
    private const val RG_ALBUM_PEAK = 3

    fun read(path: String): ReplayGainInfo? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val trackGain = readValue(retriever, RG_TRACK_GAIN)
            val albumGain = readValue(retriever, RG_ALBUM_GAIN)
            val trackPeak = readValue(retriever, RG_TRACK_PEAK)
            val albumPeak = readValue(retriever, RG_ALBUM_PEAK)
            if (trackGain == null && albumGain == null) null
            else ReplayGainInfo(trackGain, albumGain, trackPeak, albumPeak)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun readValue(retriever: MediaMetadataRetriever, index: Int): Float? {
        val raw = retriever.extractMetadata(index) ?: return null
        val cleaned = raw.trim().removeSuffix("dB").removeSuffix("db").trim()
        return cleaned.toFloatOrNull()
    }
}

class ReplayGainProcessor : BaseAudioProcessor() {

    @Volatile
    private var currentGainLinear: Float = 1f

    fun setGainDb(gainDb: Float) {
        currentGainLinear = 10f.pow(gainDb / 20f)
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        return if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            inputAudioFormat
        } else {
            AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        val gain = currentGainLinear
        val output = replaceOutputBuffer(remaining)
        output.order(ByteOrder.LITTLE_ENDIAN)

        if (gain == 1f) {
            output.put(inputBuffer)
        } else {
            val shorts = remaining / 2
            val inOrder = inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until shorts) {
                val sample = inOrder.short
                val scaled = (sample * gain).toInt().coerceIn(-32768, 32767)
                output.putShort(scaled.toShort())
            }
        }
        output.flip()
        inputBuffer.position(inputBuffer.limit())
    }

    override fun onReset() {
        currentGainLinear = 1f
    }
}

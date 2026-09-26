package com.localmusic.player.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.localmusic.player.R
import com.localmusic.player.data.db.SongEntity
import java.io.File

data class AudioFileInfo(
    val path: String,
    val sizeBytes: Long,
    val mimeType: String,
    val bitrateKbps: Int,
    val sampleRateHz: Int,
    val channels: Int,
    val durationMs: Long,
)

object SongActions {

    fun shareSong(context: Context, song: SongEntity) {
        val file = File(song.path)
        if (!file.exists()) {
            toast(context, context.getString(R.string.song_action_file_missing))
            return
        }
        val uri: Uri = runCatching {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        }.getOrElse {
            android.net.Uri.fromFile(file)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, song.title)
            putExtra(Intent.EXTRA_TEXT, "${song.title} - ${song.artist}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.song_action_share_title)))
        }.onFailure { toast(context, context.getString(R.string.song_action_no_share_app)) }
    }

    fun setAsRingtone(context: Context, song: SongEntity): Boolean {
        val file = File(song.path)
        if (!file.exists()) {
            toast(context, context.getString(R.string.song_action_file_missing))
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
            toast(context, context.getString(R.string.song_action_need_write_settings))
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
            return false
        }
        return runCatching {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DATA, file.absolutePath)
                put(MediaStore.MediaColumns.TITLE, song.title)
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
                put(MediaStore.MediaColumns.SIZE, file.length())
                put(MediaStore.Audio.Media.IS_RINGTONE, true)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
                put(MediaStore.Audio.Media.IS_ALARM, false)
                put(MediaStore.Audio.Media.IS_MUSIC, false)
            }
            val uri = MediaStore.Audio.Media.getContentUriForPath(file.absolutePath)
            val newUri = context.contentResolver.insert(uri!!, values)
                ?: throw IllegalStateException("insert failed")
            RingtoneManager.setActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_RINGTONE,
                newUri,
            )
            toast(context, context.getString(R.string.song_action_ringtone_set))
            true
        }.getOrElse {
            toast(context, context.getString(R.string.song_action_ringtone_failed))
            false
        }
    }

    fun deleteFile(context: Context, song: SongEntity): Boolean {
        val file = File(song.path)
        val deleted = if (file.exists()) file.delete() else true
        if (deleted) {
            context.contentResolver.delete(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                "${MediaStore.Audio.Media.DATA} = ?",
                arrayOf(song.path),
            )
            toast(context, context.getString(R.string.song_action_deleted))
        } else {
            toast(context, context.getString(R.string.song_action_delete_failed))
        }
        return deleted
    }

    fun readFileInfo(song: SongEntity): AudioFileInfo {
        val file = File(song.path)
        val retriever = MediaMetadataRetriever()
        var bitrate = 0
        var sampleRate = 0
        var channels = 0
        runCatching {
            retriever.setDataSource(song.path)
            bitrate = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_BITRATE
            )?.toIntOrNull() ?: 0
            sampleRate = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_SAMPLERATE
            )?.toIntOrNull() ?: 0
            channels = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS
            )?.toIntOrNull() ?: 0
        }
        runCatching { retriever.release() }
        return AudioFileInfo(
            path = song.path,
            sizeBytes = if (file.exists()) file.length() else song.size,
            mimeType = song.mimeType,
            bitrateKbps = bitrate / 1000,
            sampleRateHz = sampleRate,
            channels = channels,
            durationMs = song.duration,
        )
    }

    private fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

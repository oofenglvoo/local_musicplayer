package com.localmusic.player.ui.song

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.localmusic.player.R
import com.localmusic.player.data.ArtworkCache
import com.localmusic.player.data.db.SongEntity
import com.localmusic.player.ui.SongArtwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtworkPickerSheet(
    song: SongEntity,
    onDismiss: () -> Unit,
    onPicked: (String) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var previewUri by remember { mutableStateOf<Uri?>(null) }
    var saving by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) previewUri = uri
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.artwork_change_title), style = MaterialTheme.typography.titleLarge)

            if (previewUri != null) {
                androidx.compose.foundation.Image(
                    bitmap = remember(previewUri) {
                        runCatching {
                            context.contentResolver.openInputStream(previewUri!!)?.use {
                                BitmapFactory.decodeStream(it)
                            }
                        }.getOrNull() ?: return@remember null
                    }?.asImageBitmap()!!,
                    contentDescription = null,
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
            } else {
                SongArtwork(
                    song = song,
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { picker.launch("image/*") }) {
                    Text(stringResource(R.string.artwork_pick_image))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
                Button(
                    enabled = previewUri != null && !saving,
                    onClick = {
                        val uri = previewUri ?: return@Button
                        saving = true
                        scope.launch {
                            val saved = withContext(Dispatchers.IO) {
                                saveArtwork(context, uri, song.id)
                            }
                            saving = false
                            if (saved != null) onPicked(saved) else onDismiss()
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text(stringResource(if (saving) R.string.artwork_saving else R.string.common_save)) }
            }
        }
    }
}

private fun saveArtwork(context: Context, uri: Uri, songId: Long): String? {
    return runCatching {
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: return null
        val target = File(ArtworkCache.artworkDir(context), "manual_$songId.jpg")
        target.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bitmap.recycle()
        target.absolutePath
    }.getOrNull()
}

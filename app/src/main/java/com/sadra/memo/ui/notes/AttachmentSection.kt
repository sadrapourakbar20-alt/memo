package com.sadra.memo.ui.notes

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.sadra.memo.data.attachments.AttachmentManager
import com.sadra.memo.data.attachments.AudioRecorder
import com.sadra.memo.domain.model.Attachment
import java.io.File

private fun openFile(context: Context, file: File, mime: String) {
    try {
        val uri: Uri = FileProvider.getUriForFile(context, "com.sadra.memo.files", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // برنامه‌ای برای باز کردن این نوع فایل نیست
    } catch (e: IllegalArgumentException) {
        // فایل وجود نداره
    }
}

@Composable
fun AttachmentSection(
    attachments: List<Attachment>,
    manager: AttachmentManager,
    onPicked: (Uri) -> Unit,
    onRecorded: (File) -> Unit,
    onRemove: (Attachment) -> Unit
) {
    val context = LocalContext.current
    val recorder = remember { AudioRecorder(context) }
    var recording by remember { mutableStateOf(false) }
    var recFile by remember { mutableStateOf<File?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingPath by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            player?.release()
            if (recorder.isRecording) {
                recorder.stop()
                recFile?.delete()
            }
        }
    }

    fun stopPlayback() {
        player?.release()
        player = null
        playingPath = null
    }

    fun togglePlay(a: Attachment) {
        if (playingPath == a.path) {
            stopPlayback()
            return
        }
        stopPlayback()
        try {
            val mp = MediaPlayer()
            mp.setDataSource(manager.fileOf(a).absolutePath)
            mp.setOnCompletionListener { stopPlayback() }
            mp.prepare()
            mp.start()
            player = mp
            playingPath = a.path
        } catch (e: Exception) {
            info = "پخش فایل صوتی ممکن نشد."
        }
    }

    fun startRecording() {
        val f = manager.newAudioFile()
        if (recorder.start(f)) {
            recFile = f
            recording = true
            info = null
        } else {
            info = "ضبط صدا شروع نشد."
        }
    }

    fun stopRecording() {
        val ok = recorder.stop()
        recording = false
        val f = recFile
        recFile = null
        if (f != null) {
            if (ok) onRecorded(f) else {
                f.delete()
                info = "ضبط خیلی کوتاه بود."
            }
        }
    }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onPicked(uri)
    }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else info = "مجوز میکروفون داده نشد."
    }

    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text("پیوست‌ها", style = MaterialTheme.typography.titleSmall)
        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pickLauncher.launch(arrayOf("*/*")) }) { Text("افزودن فایل یا عکس") }
            OutlinedButton(onClick = {
                if (recording) {
                    stopRecording()
                } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    startRecording()
                } else {
                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }) { Text(if (recording) "توقف ضبط" else "ضبط صدا") }
        }
        val msg = info
        if (msg != null) {
            Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        attachments.forEach { a ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = (if (a.kind == 2) "صدا: " else if (a.kind == 1) "تصویر: " else "فایل: ") + a.name,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (a.kind == 2) {
                    TextButton(onClick = { togglePlay(a) }) {
                        Text(if (playingPath == a.path) "توقف" else "پخش")
                    }
                } else {
                    TextButton(onClick = { openFile(context, manager.fileOf(a), a.mime) }) { Text("باز کردن") }
                }
                TextButton(onClick = { if (playingPath == a.path) stopPlayback(); onRemove(a) }) { Text("حذف") }
            }
        }
    }
}

package com.sadra.memo.data.attachments

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.sadra.memo.data.dao.AttachmentDao
import com.sadra.memo.data.entities.toDomain
import com.sadra.memo.data.entities.toEntity
import com.sadra.memo.domain.model.Attachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class AttachmentManager(
    private val context: Context,
    private val dao: AttachmentDao
) {
    private val dir: File = File(context.filesDir, "attachments").apply { mkdirs() }

    fun fileOf(a: Attachment): File = File(dir, a.path)

    fun newAudioFile(): File = File(dir, "audio_" + UUID.randomUUID().toString() + ".m4a")

    suspend fun list(noteId: Long): List<Attachment> =
        dao.listForNote(noteId).map { it.toDomain() }

    /** فایل رو از Uri داخل حافظه‌ی برنامه کپی می‌کنه. noteId بعداً موقع ذخیره‌ی یادداشت ست می‌شه. */
    suspend fun copyFromUri(uri: Uri): Attachment? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            var name = "file"
            resolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) name = c.getString(idx) ?: name
            }
            val mime = resolver.getType(uri) ?: "application/octet-stream"
            val safe = name.replace(Regex("""[^\p{L}\p{N}._-]"""), "_").takeLast(60)
            val target = File(dir, UUID.randomUUID().toString().take(8) + "_" + safe)
            resolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            val kind = when {
                mime.startsWith("image/") -> 1
                mime.startsWith("audio/") -> 2
                else -> 0
            }
            Attachment(0, 0, kind, name, target.name, mime, System.currentTimeMillis())
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveForNote(noteId: Long, attachment: Attachment) {
        dao.upsert(attachment.copy(noteId = noteId).toEntity())
    }

    suspend fun remove(attachment: Attachment) = withContext(Dispatchers.IO) {
        if (attachment.id != 0L) dao.deleteById(attachment.id)
        File(dir, attachment.path).delete()
    }

    suspend fun deleteForNote(noteId: Long) = withContext(Dispatchers.IO) {
        dao.listForNote(noteId).forEach { File(dir, it.path).delete() }
        dao.deleteForNote(noteId)
    }
}

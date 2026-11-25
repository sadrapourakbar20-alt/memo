package com.sadra.memo.ui.notes

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadra.memo.data.attachments.AttachmentManager
import com.sadra.memo.domain.model.Attachment
import com.sadra.memo.domain.model.Note
import com.sadra.memo.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

data class EditState(
    val title: String = "",
    val content: String = "",
    val category: String = "",
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val loaded: Boolean = false
)

class NoteEditViewModel(
    private val repository: NoteRepository,
    private val attachmentManager: AttachmentManager,
    private val noteId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(EditState())
    val state: StateFlow<EditState> = _state

    private val _attachments = MutableStateFlow<List<Attachment>>(emptyList())
    val attachments: StateFlow<List<Attachment>> = _attachments

    private val removed = mutableListOf<Attachment>()
    private var original: Note? = null

    init {
        if (noteId > 0) {
            viewModelScope.launch {
                val note = repository.getNote(noteId)
                original = note
                _attachments.value = attachmentManager.list(noteId)
                _state.value = if (note != null) {
                    EditState(note.title, note.content, note.category, note.isPinned, note.isArchived, loaded = true)
                } else {
                    EditState(loaded = true)
                }
            }
        } else {
            _state.value = EditState(loaded = true)
        }
    }

    fun onTitle(value: String) { _state.value = _state.value.copy(title = value) }
    fun onContent(value: String) { _state.value = _state.value.copy(content = value) }
    fun onCategory(value: String) { _state.value = _state.value.copy(category = value) }
    fun onPinned(value: Boolean) { _state.value = _state.value.copy(pinned = value) }

    fun appendContent(text: String) {
        val cur = _state.value.content
        _state.value = _state.value.copy(content = if (cur.isBlank()) text else cur.trimEnd() + "\n" + text)
    }

    fun addFromUri(uri: Uri) {
        viewModelScope.launch {
            val att = attachmentManager.copyFromUri(uri)
            if (att != null) _attachments.value = _attachments.value + att
        }
    }

    fun addRecorded(file: File) {
        val att = Attachment(
            id = 0, noteId = 0, kind = 2, name = "ضبط صدا", path = file.name,
            mime = "audio/mp4", createdAt = System.currentTimeMillis()
        )
        _attachments.value = _attachments.value + att
    }

    fun removeAttachment(att: Attachment) {
        _attachments.value = _attachments.value.filter { it !== att }
        if (att.id != 0L) removed.add(att) else viewModelScope.launch { attachmentManager.remove(att) }
    }

    fun saveAndClose(onDone: () -> Unit) = persist(onDone, archive = false)

    fun archiveAndClose(onDone: () -> Unit) = persist(onDone, archive = true)

    private fun persist(onDone: () -> Unit, archive: Boolean) {
        val s = _state.value
        val atts = _attachments.value
        if (!s.loaded || (s.title.isBlank() && s.content.isBlank() && atts.isEmpty())) {
            onDone()
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val old = original
            val savedId = repository.save(
                Note(
                    id = old?.id ?: 0,
                    title = s.title.trim(),
                    content = s.content,
                    category = s.category.trim(),
                    isPinned = if (archive) false else s.pinned,
                    createdAt = old?.createdAt ?: now,
                    updatedAt = now,
                    isDeleted = old?.isDeleted ?: false,
                    isArchived = archive || (old?.isArchived ?: false)
                )
            )
            val finalId = old?.id ?: savedId
            atts.filter { it.id == 0L }.forEach { attachmentManager.saveForNote(finalId, it) }
            removed.forEach { attachmentManager.remove(it) }
            onDone()
        }
    }

    fun deleteAndClose(onDone: () -> Unit) {
        viewModelScope.launch {
            if (noteId > 0) repository.moveToTrash(noteId)
            onDone()
        }
    }
}

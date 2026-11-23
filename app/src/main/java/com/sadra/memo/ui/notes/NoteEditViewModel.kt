package com.sadra.memo.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadra.memo.domain.model.Note
import com.sadra.memo.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class EditState(
    val title: String = "",
    val content: String = "",
    val category: String = "",
    val pinned: Boolean = false,
    val loaded: Boolean = false
)

class NoteEditViewModel(
    private val repository: NoteRepository,
    private val noteId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(EditState())
    val state: StateFlow<EditState> = _state

    private var original: Note? = null

    init {
        if (noteId > 0) {
            viewModelScope.launch {
                val note = repository.getNote(noteId)
                original = note
                _state.value = if (note != null) {
                    EditState(note.title, note.content, note.category, note.isPinned, loaded = true)
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

    fun saveAndClose(onDone: () -> Unit) {
        val s = _state.value
        if (!s.loaded || (s.title.isBlank() && s.content.isBlank())) {
            onDone()
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val old = original
            repository.save(
                Note(
                    id = old?.id ?: 0,
                    title = s.title.trim(),
                    content = s.content,
                    category = s.category.trim(),
                    isPinned = s.pinned,
                    createdAt = old?.createdAt ?: now,
                    updatedAt = now
                )
            )
            onDone()
        }
    }

    fun deleteAndClose(onDone: () -> Unit) {
        viewModelScope.launch {
            if (noteId > 0) repository.delete(noteId)
            onDone()
        }
    }
}

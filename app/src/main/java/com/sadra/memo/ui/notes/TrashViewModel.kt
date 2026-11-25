package com.sadra.memo.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadra.memo.data.attachments.AttachmentManager
import com.sadra.memo.domain.model.Note
import com.sadra.memo.domain.repository.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashViewModel(
    private val repository: NoteRepository,
    private val attachments: AttachmentManager
) : ViewModel() {
    val notes: StateFlow<List<Note>> = repository.observeTrash()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restore(id: Long) {
        viewModelScope.launch { repository.restore(id) }
    }

    fun deleteForever(id: Long) {
        viewModelScope.launch {
            repository.deletePermanently(id)
            attachments.deleteForNote(id)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val ids = notes.value.map { it.id }
            repository.emptyTrash()
            ids.forEach { attachments.deleteForNote(it) }
        }
    }
}

package com.sadra.memo.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadra.memo.domain.model.Note
import com.sadra.memo.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val notes: StateFlow<List<Note>> =
        combine(repository.observeNotes(), _query) { list, q ->
            val text = q.trim()
            if (text.isEmpty()) list
            else list.filter {
                it.title.contains(text, ignoreCase = true) ||
                    it.content.contains(text, ignoreCase = true) ||
                    it.category.contains(text, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.save(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }
}

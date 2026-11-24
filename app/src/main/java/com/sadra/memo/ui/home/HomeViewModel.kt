package com.sadra.memo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadra.memo.domain.model.Note
import com.sadra.memo.domain.model.Task
import com.sadra.memo.domain.repository.NoteRepository
import com.sadra.memo.domain.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeState(
    val recentNotes: List<Note> = emptyList(),
    val tasks: List<Task> = emptyList()
)

class HomeViewModel(
    noteRepository: NoteRepository,
    taskRepository: TaskRepository
) : ViewModel() {
    val state: StateFlow<HomeState> = combine(
        noteRepository.observeNotes(),
        taskRepository.observeAll()
    ) { notes, tasks -> HomeState(notes.sortedByDescending { it.updatedAt }.take(3), tasks) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())
}

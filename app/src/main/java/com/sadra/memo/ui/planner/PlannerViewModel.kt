package com.sadra.memo.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadra.memo.domain.model.Task
import com.sadra.memo.domain.repository.TaskRepository
import com.sadra.memo.reminder.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlannerViewModel(
    private val repository: TaskRepository,
    private val scheduler: ReminderScheduler
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleDone(task: Task) {
        viewModelScope.launch {
            val done = !task.isDone
            repository.save(task.copy(isDone = done, progress = if (done) 100 else task.progress))
            val due = task.dueAt
            if (done || due == null) scheduler.cancel(task.id) else scheduler.schedule(task.id, task.title, due)
        }
    }
}

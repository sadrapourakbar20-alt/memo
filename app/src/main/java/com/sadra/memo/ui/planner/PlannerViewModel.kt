package com.sadra.memo.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadra.memo.domain.model.Task
import com.sadra.memo.domain.repository.TaskRepository
import com.sadra.memo.domain.usecase.TaskManager
import com.sadra.memo.util.Jalali
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class PlannerViewModel(
    repository: TaskRepository,
    private val manager: TaskManager
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleDone(task: Task) {
        viewModelScope.launch { manager.setDone(task, !task.isDone) }
    }

    /** ساعت قبلی کار رو نگه می‌داره و فقط روز رو عوض می‌کنه */
    fun postpone(task: Task, date: LocalDate) {
        viewModelScope.launch {
            val oldTime = task.dueAt?.let { Jalali.dateTimeOf(it).toLocalTime() } ?: LocalTime.of(9, 0)
            val zone = ZoneId.systemDefault()
            var millis = LocalDateTime.of(date, oldTime).atZone(zone).toInstant().toEpochMilli()
            if (millis <= System.currentTimeMillis()) {
                millis = LocalDateTime.of(date, LocalTime.of(23, 59)).atZone(zone).toInstant().toEpochMilli()
            }
            manager.postpone(task, millis)
        }
    }
}

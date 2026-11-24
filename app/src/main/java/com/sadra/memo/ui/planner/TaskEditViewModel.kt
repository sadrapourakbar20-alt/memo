package com.sadra.memo.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadra.memo.domain.model.Subtask
import com.sadra.memo.domain.model.Task
import com.sadra.memo.domain.repository.TaskRepository
import com.sadra.memo.domain.usecase.TaskManager
import com.sadra.memo.util.JDate
import com.sadra.memo.util.Jalali
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TaskEditState(
    val title: String = "",
    val description: String = "",
    val priority: Int = 1,
    val date: JDate? = null,
    val hour: Int = 9,
    val minute: Int = 0,
    val progress: Int = 0,
    val isDone: Boolean = false,
    val repeatType: Int = 0,
    val offsets: List<Int> = listOf(0),
    val subtasks: List<Subtask> = emptyList(),
    val loaded: Boolean = false
)

class TaskEditViewModel(
    private val repository: TaskRepository,
    private val manager: TaskManager,
    private val taskId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(TaskEditState())
    val state: StateFlow<TaskEditState> = _state

    private var original: Task? = null

    init {
        if (taskId > 0) {
            viewModelScope.launch {
                val task = repository.getTask(taskId)
                original = task
                _state.value = if (task != null) {
                    val due = task.dueAt
                    val dt = if (due != null) Jalali.dateTimeOf(due) else null
                    TaskEditState(
                        title = task.title,
                        description = task.description,
                        priority = task.priority,
                        date = dt?.let { Jalali.fromLocalDate(it.toLocalDate()) },
                        hour = dt?.hour ?: 9,
                        minute = dt?.minute ?: 0,
                        progress = task.progress,
                        isDone = task.isDone,
                        repeatType = task.repeatType,
                        offsets = task.alertOffsets,
                        subtasks = task.subtasks,
                        loaded = true
                    )
                } else {
                    TaskEditState(loaded = true)
                }
            }
        } else {
            _state.value = TaskEditState(loaded = true)
        }
    }

    fun onTitle(v: String) { _state.value = _state.value.copy(title = v) }
    fun onDescription(v: String) { _state.value = _state.value.copy(description = v) }
    fun onPriority(v: Int) { _state.value = _state.value.copy(priority = v) }
    fun onDate(v: JDate?) { _state.value = _state.value.copy(date = v) }
    fun onTime(h: Int, m: Int) { _state.value = _state.value.copy(hour = h, minute = m) }
    fun onRepeat(v: Int) { _state.value = _state.value.copy(repeatType = v) }

    fun toggleOffset(v: Int) {
        val cur = _state.value.offsets
        val next = if (v in cur) cur - v else (cur + v).sorted()
        _state.value = _state.value.copy(offsets = next.ifEmpty { listOf(0) })
    }

    fun onProgress(v: Int) {
        _state.value = _state.value.copy(progress = v, isDone = v >= 100)
    }

    private fun withSubtasks(list: List<Subtask>) {
        val s = _state.value
        if (list.isEmpty()) {
            _state.value = s.copy(subtasks = list)
        } else {
            val progress = list.count { it.done } * 100 / list.size
            _state.value = s.copy(subtasks = list, progress = progress, isDone = progress >= 100)
        }
    }

    fun addSubtask(title: String) {
        if (title.isBlank()) return
        withSubtasks(_state.value.subtasks + Subtask(title.trim(), false))
    }

    fun toggleSubtask(index: Int) {
        val list = _state.value.subtasks.toMutableList()
        if (index !in list.indices) return
        list[index] = list[index].copy(done = !list[index].done)
        withSubtasks(list)
    }

    fun removeSubtask(index: Int) {
        val list = _state.value.subtasks.toMutableList()
        if (index !in list.indices) return
        list.removeAt(index)
        withSubtasks(list)
    }

    fun saveAndClose(onDone: () -> Unit) {
        val s = _state.value
        if (!s.loaded || s.title.isBlank()) {
            onDone()
            return
        }
        viewModelScope.launch {
            val old = original
            val dueAt = s.date?.let { Jalali.toMillis(it, s.hour, s.minute) }
            val task = Task(
                id = old?.id ?: 0,
                title = s.title.trim(),
                description = s.description,
                dueAt = dueAt,
                priority = s.priority,
                progress = s.progress,
                isDone = s.isDone,
                createdAt = old?.createdAt ?: System.currentTimeMillis(),
                repeatType = s.repeatType,
                alertOffsets = s.offsets,
                subtasks = s.subtasks
            )
            manager.save(task)
            if (s.isDone && old?.isDone != true) manager.spawnNext(task)
            onDone()
        }
    }

    fun deleteAndClose(onDone: () -> Unit) {
        viewModelScope.launch {
            if (taskId > 0) manager.delete(taskId)
            onDone()
        }
    }
}

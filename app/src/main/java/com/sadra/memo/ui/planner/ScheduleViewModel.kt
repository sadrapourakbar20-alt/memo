package com.sadra.memo.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadra.memo.data.preferences.SettingsRepository
import com.sadra.memo.domain.model.ClassItem
import com.sadra.memo.domain.model.SemesterConfig
import com.sadra.memo.domain.model.Task
import com.sadra.memo.domain.repository.ClassRepository
import com.sadra.memo.domain.repository.TaskRepository
import com.sadra.memo.reminder.ClassScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ScheduleViewModel(
    private val classRepository: ClassRepository,
    taskRepository: TaskRepository,
    private val settings: SettingsRepository,
    private val classScheduler: ClassScheduler
) : ViewModel() {

    val classes: StateFlow<List<ClassItem>> = classRepository.observeClasses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cancellations: StateFlow<Set<Pair<Long, Long>>> = classRepository.observeCancellations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val tasks: StateFlow<List<Task>> = taskRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val semester: StateFlow<SemesterConfig> = settings.semesterConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SemesterConfig())

    fun setStart(date: LocalDate?) {
        viewModelScope.launch {
            settings.setSemesterStart(date)
            classScheduler.rescheduleAll()
        }
    }

    fun setEnd(date: LocalDate?) {
        viewModelScope.launch {
            settings.setSemesterEnd(date)
            classScheduler.rescheduleAll()
        }
    }

    fun setInvert(value: Boolean) {
        viewModelScope.launch {
            settings.setInvertParity(value)
            classScheduler.rescheduleAll()
        }
    }

    fun toggleCancel(classId: Long, date: LocalDate) {
        viewModelScope.launch {
            classRepository.toggleCancellation(classId, date.toEpochDay())
            classScheduler.rescheduleAll()
        }
    }
}

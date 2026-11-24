package com.sadra.memo.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadra.memo.domain.model.ClassItem
import com.sadra.memo.domain.repository.ClassRepository
import com.sadra.memo.reminder.ClassScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ClassEditState(
    val name: String = "",
    val teacher: String = "",
    val location: String = "",
    val weekday: Int = 0,
    val start: Int = 8 * 60,
    val end: Int = 10 * 60,
    val pattern: Int = 0,
    val offsets: List<Int> = listOf(30),
    val error: String? = null,
    val loaded: Boolean = false
)

class ClassEditViewModel(
    private val repository: ClassRepository,
    private val scheduler: ClassScheduler,
    private val classId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(ClassEditState())
    val state: StateFlow<ClassEditState> = _state

    init {
        if (classId > 0) {
            viewModelScope.launch {
                val c = repository.getClass(classId)
                _state.value = if (c != null) {
                    ClassEditState(
                        name = c.name, teacher = c.teacher, location = c.location,
                        weekday = c.weekday, start = c.startMinutes, end = c.endMinutes,
                        pattern = c.pattern, offsets = c.alertOffsets, loaded = true
                    )
                } else ClassEditState(loaded = true)
            }
        } else {
            _state.value = ClassEditState(loaded = true)
        }
    }

    fun onName(v: String) { _state.value = _state.value.copy(name = v, error = null) }
    fun onTeacher(v: String) { _state.value = _state.value.copy(teacher = v) }
    fun onLocation(v: String) { _state.value = _state.value.copy(location = v) }
    fun onWeekday(v: Int) { _state.value = _state.value.copy(weekday = v) }
    fun onStart(h: Int, m: Int) { _state.value = _state.value.copy(start = h * 60 + m, error = null) }
    fun onEnd(h: Int, m: Int) { _state.value = _state.value.copy(end = h * 60 + m, error = null) }
    fun onPattern(v: Int) { _state.value = _state.value.copy(pattern = v) }
    fun toggleOffset(v: Int) {
        val cur = _state.value.offsets
        _state.value = _state.value.copy(offsets = if (v in cur) cur - v else (cur + v).sorted())
    }

    fun saveAndClose(onDone: () -> Unit) {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.value = s.copy(error = "اسم درس رو وارد کن.")
            return
        }
        if (s.end <= s.start) {
            _state.value = s.copy(error = "ساعت پایان باید بعد از ساعت شروع باشه.")
            return
        }
        viewModelScope.launch {
            repository.save(
                ClassItem(
                    id = if (classId > 0) classId else 0,
                    name = s.name.trim(), teacher = s.teacher.trim(), location = s.location.trim(),
                    weekday = s.weekday, startMinutes = s.start, endMinutes = s.end,
                    pattern = s.pattern, alertOffsets = s.offsets
                )
            )
            scheduler.rescheduleAll()
            onDone()
        }
    }

    fun deleteAndClose(onDone: () -> Unit) {
        viewModelScope.launch {
            if (classId > 0) {
                repository.delete(classId)
                scheduler.cancelClass(classId)
            }
            onDone()
        }
    }
}

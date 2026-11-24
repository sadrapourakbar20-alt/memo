package com.sadra.memo.domain.usecase

import com.sadra.memo.domain.model.Task
import com.sadra.memo.domain.repository.TaskRepository
import com.sadra.memo.domain.schedule.Recurrence
import com.sadra.memo.reminder.ReminderScheduler

class TaskManager(
    private val repository: TaskRepository,
    private val scheduler: ReminderScheduler
) {
    suspend fun save(task: Task): Long {
        val id = repository.save(task)
        val finalId = if (task.id != 0L) task.id else id
        scheduler.cancelTask(finalId)
        val due = task.dueAt
        if (!task.isDone && due != null) {
            scheduler.scheduleTask(finalId, task.title, due, task.alertOffsets)
        }
        return finalId
    }

    suspend fun setDone(task: Task, done: Boolean) {
        val updated = task.copy(
            isDone = done,
            progress = if (done) 100 else task.progress,
            subtasks = if (done) task.subtasks.map { it.copy(done = true) } else task.subtasks
        )
        save(updated)
        if (done && !task.isDone) spawnNext(updated)
    }

    /** برای کار تکرارشونده، نوبت بعدی رو می‌سازه */
    suspend fun spawnNext(task: Task) {
        val due = task.dueAt ?: return
        if (task.repeatType == Recurrence.NONE) return
        val now = System.currentTimeMillis()
        val next = Recurrence.nextDue(due, task.repeatType, now)
        save(
            task.copy(
                id = 0,
                dueAt = next,
                isDone = false,
                progress = 0,
                subtasks = task.subtasks.map { it.copy(done = false) },
                createdAt = now
            )
        )
    }

    suspend fun postpone(task: Task, newDueAt: Long) {
        save(task.copy(dueAt = newDueAt))
    }

    suspend fun delete(id: Long) {
        repository.delete(id)
        scheduler.cancelTask(id)
    }

    suspend fun rescheduleAll() {
        repository.getUpcomingWithDueDate(System.currentTimeMillis()).forEach { task ->
            scheduler.cancelTask(task.id)
            val due = task.dueAt
            if (due != null) scheduler.scheduleTask(task.id, task.title, due, task.alertOffsets)
        }
    }
}

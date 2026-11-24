package com.sadra.memo.domain.repository

import com.sadra.memo.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeAll(): Flow<List<Task>>
    suspend fun getTask(id: Long): Task?
    suspend fun save(task: Task): Long
    suspend fun delete(id: Long)
    suspend fun getUpcomingWithDueDate(now: Long): List<Task>
}

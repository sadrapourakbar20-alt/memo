package com.sadra.memo.data.repository

import com.sadra.memo.data.dao.TaskDao
import com.sadra.memo.data.entities.toDomain
import com.sadra.memo.data.entities.toEntity
import com.sadra.memo.domain.model.Task
import com.sadra.memo.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepositoryImpl(private val dao: TaskDao) : TaskRepository {
    override fun observeAll(): Flow<List<Task>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getTask(id: Long): Task? = dao.getById(id)?.toDomain()

    override suspend fun save(task: Task): Long = dao.upsert(task.toEntity())

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override suspend fun getUpcomingWithDueDate(now: Long): List<Task> =
        dao.upcomingWithDueDate(now).map { it.toDomain() }
}

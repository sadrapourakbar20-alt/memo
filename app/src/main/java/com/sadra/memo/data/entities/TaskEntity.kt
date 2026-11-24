package com.sadra.memo.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sadra.memo.domain.model.Task

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val dueAt: Long?,
    val priority: Int,
    val progress: Int,
    val isDone: Boolean,
    val createdAt: Long
)

fun TaskEntity.toDomain() = Task(id, title, description, dueAt, priority, progress, isDone, createdAt)

fun Task.toEntity() = TaskEntity(id, title, description, dueAt, priority, progress, isDone, createdAt)

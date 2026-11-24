package com.sadra.memo.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sadra.memo.domain.model.Task
import com.sadra.memo.domain.model.decodeOffsets
import com.sadra.memo.domain.model.decodeSubtasks
import com.sadra.memo.domain.model.encodeOffsets
import com.sadra.memo.domain.model.encodeSubtasks

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val dueAt: Long?,
    val priority: Int,
    val progress: Int,
    val isDone: Boolean,
    val createdAt: Long,
    val repeatType: Int = 0,
    val alertOffsets: String = "",
    val subtasks: String = ""
)

fun TaskEntity.toDomain() = Task(
    id = id,
    title = title,
    description = description,
    dueAt = dueAt,
    priority = priority,
    progress = progress,
    isDone = isDone,
    createdAt = createdAt,
    repeatType = repeatType,
    alertOffsets = decodeOffsets(alertOffsets).ifEmpty { listOf(0) },
    subtasks = decodeSubtasks(subtasks)
)

fun Task.toEntity() = TaskEntity(
    id = id,
    title = title,
    description = description,
    dueAt = dueAt,
    priority = priority,
    progress = progress,
    isDone = isDone,
    createdAt = createdAt,
    repeatType = repeatType,
    alertOffsets = encodeOffsets(alertOffsets),
    subtasks = encodeSubtasks(subtasks)
)

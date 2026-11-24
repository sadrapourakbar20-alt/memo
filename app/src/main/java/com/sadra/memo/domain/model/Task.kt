package com.sadra.memo.domain.model

data class Task(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueAt: Long? = null,
    val priority: Int = 1,
    val progress: Int = 0,
    val isDone: Boolean = false,
    val createdAt: Long
)

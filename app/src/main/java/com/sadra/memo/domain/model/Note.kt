package com.sadra.memo.domain.model

data class Note(
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "",
    val isPinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val isArchived: Boolean = false
)

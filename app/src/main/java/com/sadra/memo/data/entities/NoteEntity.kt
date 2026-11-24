package com.sadra.memo.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sadra.memo.domain.model.Note

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val category: String,
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

fun NoteEntity.toDomain() = Note(id, title, content, category, isPinned, createdAt, updatedAt, isDeleted)

fun Note.toEntity() = NoteEntity(id, title, content, category, isPinned, createdAt, updatedAt, isDeleted)

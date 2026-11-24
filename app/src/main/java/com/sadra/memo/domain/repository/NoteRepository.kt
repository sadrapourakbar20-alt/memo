package com.sadra.memo.domain.repository

import com.sadra.memo.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(): Flow<List<Note>>
    fun observeTrash(): Flow<List<Note>>
    suspend fun getNote(id: Long): Note?
    suspend fun save(note: Note): Long
    suspend fun moveToTrash(id: Long)
    suspend fun restore(id: Long)
    suspend fun deletePermanently(id: Long)
    suspend fun emptyTrash()
}

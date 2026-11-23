package com.sadra.memo.domain.repository

import com.sadra.memo.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(): Flow<List<Note>>
    suspend fun getNote(id: Long): Note?
    suspend fun save(note: Note): Long
    suspend fun delete(id: Long)
}

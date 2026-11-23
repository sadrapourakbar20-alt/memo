package com.sadra.memo.data.repository

import com.sadra.memo.data.dao.NoteDao
import com.sadra.memo.data.entities.toDomain
import com.sadra.memo.data.entities.toEntity
import com.sadra.memo.domain.model.Note
import com.sadra.memo.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl(private val dao: NoteDao) : NoteRepository {
    override fun observeNotes(): Flow<List<Note>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getNote(id: Long): Note? = dao.getById(id)?.toDomain()

    override suspend fun save(note: Note): Long = dao.upsert(note.toEntity())

    override suspend fun delete(id: Long) = dao.deleteById(id)
}

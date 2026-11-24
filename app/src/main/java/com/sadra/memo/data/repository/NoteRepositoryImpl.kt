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
        dao.observeActive().map { list -> list.map { it.toDomain() } }

    override fun observeTrash(): Flow<List<Note>> =
        dao.observeTrash().map { list -> list.map { it.toDomain() } }

    override fun observeArchived(): Flow<List<Note>> =
        dao.observeArchived().map { list -> list.map { it.toDomain() } }

    override suspend fun getNote(id: Long): Note? = dao.getById(id)?.toDomain()

    override suspend fun save(note: Note): Long = dao.upsert(note.toEntity())

    override suspend fun moveToTrash(id: Long) = dao.softDelete(id, System.currentTimeMillis())

    override suspend fun restore(id: Long) = dao.restore(id, System.currentTimeMillis())

    override suspend fun setArchived(id: Long, archived: Boolean) =
        dao.setArchived(id, archived, System.currentTimeMillis())

    override suspend fun deletePermanently(id: Long) = dao.deleteById(id)

    override suspend fun emptyTrash() = dao.emptyTrash()
}

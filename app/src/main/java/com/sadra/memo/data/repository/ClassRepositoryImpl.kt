package com.sadra.memo.data.repository

import com.sadra.memo.data.dao.ClassDao
import com.sadra.memo.data.entities.CancellationEntity
import com.sadra.memo.data.entities.toDomain
import com.sadra.memo.data.entities.toEntity
import com.sadra.memo.domain.model.ClassItem
import com.sadra.memo.domain.repository.ClassRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ClassRepositoryImpl(private val dao: ClassDao) : ClassRepository {
    override fun observeClasses(): Flow<List<ClassItem>> =
        dao.observeClasses().map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<ClassItem> = dao.getAll().map { it.toDomain() }

    override suspend fun getClass(id: Long): ClassItem? = dao.getById(id)?.toDomain()

    override suspend fun save(item: ClassItem): Long = dao.upsert(item.toEntity())

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
        dao.deleteCancellationsOf(id)
    }

    override fun observeCancellations(): Flow<Set<Pair<Long, Long>>> =
        dao.observeCancellations().map { list -> list.map { it.classId to it.epochDay }.toSet() }

    override suspend fun getAllCancellations(): Set<Pair<Long, Long>> =
        dao.getAllCancellations().map { it.classId to it.epochDay }.toSet()

    override suspend fun toggleCancellation(classId: Long, epochDay: Long) {
        val existing = dao.findCancellation(classId, epochDay)
        if (existing != null) dao.deleteCancellation(existing.id)
        else dao.upsertCancellation(CancellationEntity(classId = classId, epochDay = epochDay))
    }
}

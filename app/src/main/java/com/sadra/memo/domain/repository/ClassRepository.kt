package com.sadra.memo.domain.repository

import com.sadra.memo.domain.model.ClassItem
import kotlinx.coroutines.flow.Flow

interface ClassRepository {
    fun observeClasses(): Flow<List<ClassItem>>
    suspend fun getAll(): List<ClassItem>
    suspend fun getClass(id: Long): ClassItem?
    suspend fun save(item: ClassItem): Long
    suspend fun delete(id: Long)
    fun observeCancellations(): Flow<Set<Pair<Long, Long>>>
    suspend fun getAllCancellations(): Set<Pair<Long, Long>>
    suspend fun toggleCancellation(classId: Long, epochDay: Long)
}

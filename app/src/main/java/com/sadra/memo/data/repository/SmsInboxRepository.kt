package com.sadra.memo.data.repository

import com.sadra.memo.data.dao.SmsInboxDao
import com.sadra.memo.data.entities.toDomain
import com.sadra.memo.data.entities.toEntity
import com.sadra.memo.domain.model.SmsInboxItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SmsInboxRepository(private val dao: SmsInboxDao) {
    fun observeAll(): Flow<List<SmsInboxItem>> = dao.observeAll().map { l -> l.map { it.toDomain() } }
    suspend fun exists(hash: String): Boolean = dao.countByHash(hash) > 0
    suspend fun add(item: SmsInboxItem): Long = dao.insert(item.toEntity())
    suspend fun delete(id: Long) = dao.delete(id)
}

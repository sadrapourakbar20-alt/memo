package com.sadra.memo.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sadra.memo.data.entities.SmsInboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsInboxDao {
    @Query("SELECT * FROM sms_inbox ORDER BY dateMillis DESC")
    fun observeAll(): Flow<List<SmsInboxEntity>>

    @Query("SELECT COUNT(*) FROM sms_inbox WHERE hash = :hash")
    suspend fun countByHash(hash: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SmsInboxEntity): Long

    @Query("DELETE FROM sms_inbox WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM sms_inbox")
    suspend fun clearAll()
}

package com.sadra.memo.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sadra.memo.data.entities.CancellationEntity
import com.sadra.memo.data.entities.ClassEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassDao {
    @Query("SELECT * FROM classes ORDER BY weekday, startMinutes")
    fun observeClasses(): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes")
    suspend fun getAll(): List<ClassEntity>

    @Query("SELECT * FROM classes WHERE id = :id")
    suspend fun getById(id: Long): ClassEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ClassEntity): Long

    @Query("DELETE FROM classes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM class_cancellations")
    fun observeCancellations(): Flow<List<CancellationEntity>>

    @Query("SELECT * FROM class_cancellations")
    suspend fun getAllCancellations(): List<CancellationEntity>

    @Query("SELECT * FROM class_cancellations WHERE classId = :classId AND epochDay = :epochDay")
    suspend fun findCancellation(classId: Long, epochDay: Long): CancellationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCancellation(item: CancellationEntity): Long

    @Query("DELETE FROM class_cancellations WHERE id = :id")
    suspend fun deleteCancellation(id: Long)

    @Query("DELETE FROM class_cancellations WHERE classId = :classId")
    suspend fun deleteCancellationsOf(classId: Long)

    @Query("DELETE FROM classes")
    suspend fun clearClasses()

    @Query("DELETE FROM class_cancellations")
    suspend fun clearCancellations()
}

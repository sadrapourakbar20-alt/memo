package com.sadra.memo.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sadra.memo.domain.model.ClassItem
import com.sadra.memo.domain.model.decodeOffsets
import com.sadra.memo.domain.model.encodeOffsets

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val teacher: String,
    val location: String,
    val weekday: Int,
    val startMinutes: Int,
    val endMinutes: Int,
    val pattern: Int,
    val alertOffsets: String
)

@Entity(tableName = "class_cancellations")
data class CancellationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classId: Long,
    val epochDay: Long
)

fun ClassEntity.toDomain() = ClassItem(
    id, name, teacher, location, weekday, startMinutes, endMinutes, pattern, decodeOffsets(alertOffsets)
)

fun ClassItem.toEntity() = ClassEntity(
    id, name, teacher, location, weekday, startMinutes, endMinutes, pattern, encodeOffsets(alertOffsets)
)

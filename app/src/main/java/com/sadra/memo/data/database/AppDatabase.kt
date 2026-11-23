package com.sadra.memo.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sadra.memo.data.dao.NoteDao
import com.sadra.memo.data.entities.NoteEntity

@Database(entities = [NoteEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}

package com.sadra.memo.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sadra.memo.data.dao.ClassDao
import com.sadra.memo.data.dao.NoteDao
import com.sadra.memo.data.dao.TaskDao
import com.sadra.memo.data.entities.CancellationEntity
import com.sadra.memo.data.entities.ClassEntity
import com.sadra.memo.data.entities.NoteEntity
import com.sadra.memo.data.entities.TaskEntity

@Database(
    entities = [NoteEntity::class, TaskEntity::class, ClassEntity::class, CancellationEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun classDao(): ClassDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tasks` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`description` TEXT NOT NULL, " +
                "`dueAt` INTEGER, " +
                "`priority` INTEGER NOT NULL, " +
                "`progress` INTEGER NOT NULL, " +
                "`isDone` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tasks ADD COLUMN repeatType INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tasks ADD COLUMN alertOffsets TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE tasks ADD COLUMN subtasks TEXT NOT NULL DEFAULT ''")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `classes` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`teacher` TEXT NOT NULL, " +
                "`location` TEXT NOT NULL, " +
                "`weekday` INTEGER NOT NULL, " +
                "`startMinutes` INTEGER NOT NULL, " +
                "`endMinutes` INTEGER NOT NULL, " +
                "`pattern` INTEGER NOT NULL, " +
                "`alertOffsets` TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `class_cancellations` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`classId` INTEGER NOT NULL, " +
                "`epochDay` INTEGER NOT NULL)"
        )
    }
}

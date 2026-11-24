package com.sadra.memo.di

import android.content.Context
import androidx.room.Room
import com.sadra.memo.data.database.AppDatabase
import com.sadra.memo.data.database.MIGRATION_1_2
import com.sadra.memo.data.preferences.SettingsRepository
import com.sadra.memo.data.repository.NoteRepositoryImpl
import com.sadra.memo.data.repository.TaskRepositoryImpl
import com.sadra.memo.domain.repository.NoteRepository
import com.sadra.memo.domain.repository.TaskRepository
import com.sadra.memo.reminder.ReminderScheduler

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "memo.db"
    ).addMigrations(MIGRATION_1_2).build()

    val noteRepository: NoteRepository = NoteRepositoryImpl(database.noteDao())
    val taskRepository: TaskRepository = TaskRepositoryImpl(database.taskDao())
    val settingsRepository = SettingsRepository(appContext)
    val reminderScheduler = ReminderScheduler(appContext)
}

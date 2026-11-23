package com.sadra.memo.di

import android.content.Context
import androidx.room.Room
import com.sadra.memo.data.database.AppDatabase
import com.sadra.memo.data.repository.NoteRepositoryImpl
import com.sadra.memo.domain.repository.NoteRepository

class AppContainer(context: Context) {
    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "memo.db"
    ).build()

    val noteRepository: NoteRepository = NoteRepositoryImpl(database.noteDao())
}

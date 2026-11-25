package com.sadra.memo.di

import android.content.Context
import androidx.room.Room
import com.sadra.memo.data.attachments.AttachmentManager
import com.sadra.memo.data.backup.BackupManager
import com.sadra.memo.data.database.AppDatabase
import com.sadra.memo.data.database.MIGRATION_1_2
import com.sadra.memo.data.database.MIGRATION_2_3
import com.sadra.memo.data.database.MIGRATION_3_4
import com.sadra.memo.data.database.MIGRATION_4_5
import com.sadra.memo.data.preferences.SettingsRepository
import com.sadra.memo.data.repository.ClassRepositoryImpl
import com.sadra.memo.data.repository.FinanceRepositoryImpl
import com.sadra.memo.data.repository.NoteRepositoryImpl
import com.sadra.memo.data.repository.SmsInboxRepository
import com.sadra.memo.data.repository.TaskRepositoryImpl
import com.sadra.memo.domain.repository.ClassRepository
import com.sadra.memo.domain.repository.FinanceRepository
import com.sadra.memo.domain.repository.NoteRepository
import com.sadra.memo.domain.repository.TaskRepository
import com.sadra.memo.domain.usecase.TaskManager
import com.sadra.memo.reminder.ClassScheduler
import com.sadra.memo.reminder.ReminderScheduler
import com.sadra.memo.sms.SmsProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "memo.db"
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()

    val noteRepository: NoteRepository = NoteRepositoryImpl(database.noteDao())
    val taskRepository: TaskRepository = TaskRepositoryImpl(database.taskDao())
    val classRepository: ClassRepository = ClassRepositoryImpl(database.classDao())
    val financeRepository: FinanceRepository = FinanceRepositoryImpl(database.financeDao())
    val attachmentManager = AttachmentManager(appContext, database.attachmentDao())
    val smsInboxRepository = SmsInboxRepository(database.smsInboxDao())
    val settingsRepository = SettingsRepository(appContext)
    val reminderScheduler = ReminderScheduler(appContext)
    val taskManager = TaskManager(taskRepository, reminderScheduler)
    val classScheduler = ClassScheduler(classRepository, settingsRepository, reminderScheduler)
    val backupManager = BackupManager(database, settingsRepository)
    val smsProcessor = SmsProcessor(appContext, financeRepository, smsInboxRepository, settingsRepository)
}

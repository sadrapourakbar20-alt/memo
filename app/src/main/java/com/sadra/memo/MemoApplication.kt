package com.sadra.memo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.sadra.memo.di.AppContainer
import com.sadra.memo.reminder.ReminderReceiver

class MemoApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        val channel = NotificationChannel(
            ReminderReceiver.CHANNEL_ID,
            "یادآوری‌ها",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "یادآوری کارها و رویدادها" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

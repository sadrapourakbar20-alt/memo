package com.sadra.memo

import android.app.Application
import com.sadra.memo.di.AppContainer
import com.sadra.memo.reminder.NotificationHelper
import kotlinx.coroutines.launch

class MemoApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannel(this)

        container.appScope.launch {
            container.taskManager.rescheduleAll()
            container.classScheduler.rescheduleAll()
        }
    }
}

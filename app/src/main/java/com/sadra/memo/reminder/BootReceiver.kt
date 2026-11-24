package com.sadra.memo.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sadra.memo.MemoApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as MemoApplication
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = app.container.taskRepository.getUpcomingWithDueDate(System.currentTimeMillis())
                tasks.forEach { task ->
                    val due = task.dueAt
                    if (due != null) app.container.reminderScheduler.schedule(task.id, task.title, due)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

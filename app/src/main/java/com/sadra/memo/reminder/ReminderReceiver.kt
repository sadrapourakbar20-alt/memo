package com.sadra.memo.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.sadra.memo.MainActivity
import com.sadra.memo.MemoApplication
import com.sadra.memo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id < 0) return
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val app = context.applicationContext as MemoApplication
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (intent.action) {
            ACTION_FIRE -> showNotification(context, manager, id, title)
            ACTION_SNOOZE -> {
                manager.cancel(id.toInt())
                app.container.reminderScheduler.schedule(id, title, System.currentTimeMillis() + SNOOZE_MS)
            }
            ACTION_DONE -> {
                manager.cancel(id.toInt())
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = app.container.taskRepository
                        val task = repo.getTask(id)
                        if (task != null) repo.save(task.copy(isDone = true, progress = 100))
                        app.container.reminderScheduler.cancel(id)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private fun showNotification(context: Context, manager: NotificationManager, id: Long, title: String) {
        val openIntent = PendingIntent.getActivity(
            context,
            id.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val done = actionIntent(context, id, title, ACTION_DONE, 1)
        val snooze = actionIntent(context, id, title, ACTION_SNOOZE, 2)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("یادآوری")
            .setContentText(title.ifBlank { "یک کار منتظر توست" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .addAction(0, "انجام شد", done)
            .addAction(0, "۱۰ دقیقه بعد", snooze)
            .build()
        try {
            manager.notify(id.toInt(), notification)
        } catch (e: SecurityException) {
            // مجوز اعلان داده نشده؛ کاری نمی‌شه کرد
        }
    }

    private fun actionIntent(context: Context, id: Long, title: String, action: String, n: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_TITLE, title)
        }
        return PendingIntent.getBroadcast(
            context,
            (id * 10 + n).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "reminders"
        const val ACTION_FIRE = "com.sadra.memo.action.FIRE"
        const val ACTION_DONE = "com.sadra.memo.action.DONE"
        const val ACTION_SNOOZE = "com.sadra.memo.action.SNOOZE"
        const val EXTRA_ID = "task_id"
        const val EXTRA_TITLE = "task_title"
        const val SNOOZE_MS = 10 * 60 * 1000L
    }
}

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
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        val isClass = intent.getBooleanExtra(EXTRA_IS_CLASS, false)
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, id.toInt())
        val app = context.applicationContext as MemoApplication
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (intent.action) {
            ACTION_FIRE -> {
                if (isClass) {
                    show(context, manager, notifId, title, body, withActions = false, taskId = -1L)
                    val pending = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            app.container.classScheduler.rescheduleAll()
                        } finally {
                            pending.finish()
                        }
                    }
                } else {
                    show(context, manager, notifId, title, body, withActions = true, taskId = id)
                }
            }
            ACTION_SNOOZE -> {
                manager.cancel(notifId)
                app.container.reminderScheduler.scheduleSnooze(id, title, System.currentTimeMillis() + SNOOZE_MS)
            }
            ACTION_DONE -> {
                manager.cancel(notifId)
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val task = app.container.taskRepository.getTask(id)
                        if (task != null) app.container.taskManager.setDone(task, true)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private fun show(
        context: Context,
        manager: NotificationManager,
        notifId: Int,
        title: String,
        body: String,
        withActions: Boolean,
        taskId: Long
    ) {
        val openIntent = PendingIntent.getActivity(
            context,
            notifId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, NotificationHelper.channelId(context))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title.ifBlank { "یادآوری" })
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
        if (withActions) {
            builder.addAction(0, "انجام شد", actionIntent(context, taskId, title, notifId, ACTION_DONE, 1))
            builder.addAction(0, "۱۰ دقیقه بعد", actionIntent(context, taskId, title, notifId, ACTION_SNOOZE, 2))
        }
        try {
            manager.notify(notifId, builder.build())
        } catch (e: SecurityException) {
            // مجوز اعلان داده نشده
        }
    }

    private fun actionIntent(
        context: Context,
        id: Long,
        title: String,
        notifId: Int,
        action: String,
        n: Int
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_NOTIF_ID, notifId)
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
        const val EXTRA_BODY = "task_body"
        const val EXTRA_IS_CLASS = "is_class"
        const val EXTRA_NOTIF_ID = "notif_id"
        const val SNOOZE_MS = 10 * 60 * 1000L
    }
}

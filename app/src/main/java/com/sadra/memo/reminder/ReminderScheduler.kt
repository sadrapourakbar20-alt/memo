package com.sadra.memo.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sadra.memo.util.durationText

class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun buildIntent(id: Long, title: String, body: String, isClass: Boolean, notifId: Int): Intent =
        Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE
            putExtra(ReminderReceiver.EXTRA_ID, id)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_BODY, body)
            putExtra(ReminderReceiver.EXTRA_IS_CLASS, isClass)
            putExtra(ReminderReceiver.EXTRA_NOTIF_ID, notifId)
        }

    private fun setAlarm(code: Int, triggerAt: Long, intent: Intent) {
        val pi = PendingIntent.getBroadcast(
            context, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (canExact) alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            else alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancelByCode(code: Int) {
        val pi = PendingIntent.getBroadcast(
            context, code,
            Intent(context, ReminderReceiver::class.java).apply { action = ReminderReceiver.ACTION_FIRE },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    // ---- کارها ----
    private fun taskCode(taskId: Long, idx: Int): Int = taskId.toInt() * 10 + idx

    fun scheduleTask(taskId: Long, title: String, dueAt: Long, offsets: List<Int>) {
        val now = System.currentTimeMillis()
        offsets.take(MAX_ALERTS).forEachIndexed { idx, off ->
            val trigger = dueAt - off * 60_000L
            if (trigger > now) {
                val body = if (off == 0) "الان وقتشه" else "حدود " + durationText(off) + " دیگه"
                setAlarm(taskCode(taskId, idx), trigger, buildIntent(taskId, title, body, false, taskId.toInt()))
            }
        }
    }

    fun scheduleSnooze(taskId: Long, title: String, triggerAt: Long) {
        setAlarm(
            taskCode(taskId, SNOOZE_SLOT), triggerAt,
            buildIntent(taskId, title, "یادآوری تعویق‌شده", false, taskId.toInt())
        )
    }

    fun cancelTask(taskId: Long) {
        for (idx in 0 until MAX_ALERTS) cancelByCode(taskCode(taskId, idx))
        cancelByCode(taskCode(taskId, SNOOZE_SLOT))
        cancelByCode(taskId.toInt()) // آلارم‌های نسخه ۰٫۲
    }

    // ---- کلاس‌ها ----
    fun scheduleClassAlert(code: Int, title: String, body: String, triggerAt: Long) {
        setAlarm(code, triggerAt, buildIntent(-1L, title, body, true, code))
    }

    companion object {
        const val MAX_ALERTS = 5
        const val SNOOZE_SLOT = 9
    }
}

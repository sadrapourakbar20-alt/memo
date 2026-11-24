package com.sadra.memo.reminder

import com.sadra.memo.data.preferences.SettingsRepository
import com.sadra.memo.domain.repository.ClassRepository
import com.sadra.memo.domain.schedule.ClassSchedule
import com.sadra.memo.util.durationText
import com.sadra.memo.util.minutesText
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ClassScheduler(
    private val classRepository: ClassRepository,
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler
) {
    private fun code(classId: Long, epochDay: Long, idx: Int): Int =
        ((classId * 100_000L + epochDay) * 8L + idx).toInt()

    /** آلارم‌های ۳ هفته آینده رو از نو می‌چینه. بعد از هر تغییر و هر بار که یه آلارم کلاس بخوره صدا زده می‌شه. */
    suspend fun rescheduleAll() {
        val sem = settings.semesterConfig.first()
        val classes = classRepository.getAll()
        val cancelled = classRepository.getAllCancellations()
        val today = LocalDate.now()
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()

        for (c in classes) {
            for (day in 0 until WINDOW) {
                val date = today.plusDays(day.toLong())
                val epoch = date.toEpochDay()
                for (idx in 0 until ReminderScheduler.MAX_ALERTS) {
                    scheduler.cancelByCode(code(c.id, epoch, idx))
                }
                if (!ClassSchedule.occursBase(c, date, sem)) continue
                if ((c.id to epoch) in cancelled) continue
                val start = LocalDateTime.of(date, LocalTime.of(c.startMinutes / 60, c.startMinutes % 60))
                    .atZone(zone).toInstant().toEpochMilli()
                c.alertOffsets.take(ReminderScheduler.MAX_ALERTS).forEachIndexed { idx, off ->
                    val trigger = start - off * 60_000L
                    if (trigger > now) {
                        var body = "ساعت " + minutesText(c.startMinutes)
                        if (off > 0) body += " (" + durationText(off) + " دیگه)"
                        if (c.location.isNotBlank()) body += " - " + c.location
                        scheduler.scheduleClassAlert(code(c.id, epoch, idx), "کلاس " + c.name, body, trigger)
                    }
                }
            }
        }
    }

    fun cancelClass(classId: Long) {
        val today = LocalDate.now()
        for (day in 0 until WINDOW) {
            val epoch = today.plusDays(day.toLong()).toEpochDay()
            for (idx in 0 until ReminderScheduler.MAX_ALERTS) {
                scheduler.cancelByCode(code(classId, epoch, idx))
            }
        }
    }

    companion object {
        const val WINDOW = 21
    }
}

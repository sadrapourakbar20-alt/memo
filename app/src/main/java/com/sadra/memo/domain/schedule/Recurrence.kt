package com.sadra.memo.domain.schedule

import com.sadra.memo.util.JDate
import com.sadra.memo.util.Jalali
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object Recurrence {
    const val NONE = 0
    const val DAILY = 1
    const val WEEKLY = 2
    const val MONTHLY = 3
    const val YEARLY = 4

    val labels = listOf("بدون تکرار", "روزانه", "هفتگی", "ماهانه", "سالانه")

    private fun millis(dt: LocalDateTime): Long =
        dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun advance(dt: LocalDateTime, type: Int): LocalDateTime = when (type) {
        DAILY -> dt.plusDays(1)
        WEEKLY -> dt.plusWeeks(1)
        MONTHLY -> {
            val j = Jalali.fromLocalDate(dt.toLocalDate())
            val ny = if (j.month == 12) j.year + 1 else j.year
            val nm = if (j.month == 12) 1 else j.month + 1
            val day = minOf(j.day, Jalali.monthLength(ny, nm))
            val date: LocalDate = Jalali.toGregorian(JDate(ny, nm, day))
            LocalDateTime.of(date, dt.toLocalTime())
        }
        YEARLY -> {
            val j = Jalali.fromLocalDate(dt.toLocalDate())
            val ny = j.year + 1
            val day = minOf(j.day, Jalali.monthLength(ny, j.month))
            val date: LocalDate = Jalali.toGregorian(JDate(ny, j.month, day))
            LocalDateTime.of(date, dt.toLocalTime())
        }
        else -> dt
    }

    /** اولین زمانِ بعد از «now» در الگوی تکرار */
    fun nextDue(dueAt: Long, type: Int, now: Long): Long {
        var dt = Jalali.dateTimeOf(dueAt)
        var guard = 0
        do {
            dt = advance(dt, type)
            guard++
        } while (millis(dt) <= now && guard < 800 && type != NONE)
        return millis(dt)
    }
}

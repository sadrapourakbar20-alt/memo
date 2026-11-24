package com.sadra.memo.domain.schedule

import com.sadra.memo.domain.model.ClassItem
import com.sadra.memo.domain.model.SemesterConfig
import com.sadra.memo.util.Jalali
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object ClassSchedule {
    /** شنبه = ۰ ... جمعه = ۶ */
    fun weekdayIndex(d: LocalDate): Int = (d.dayOfWeek.value + 1) % 7

    fun weekStart(d: LocalDate): LocalDate = d.minusDays(weekdayIndex(d).toLong())

    fun weekNumber(d: LocalDate, semesterStart: LocalDate): Int =
        (ChronoUnit.DAYS.between(weekStart(semesterStart), weekStart(d)) / 7).toInt() + 1

    /** null یعنی شروع ترم مشخص نشده */
    fun isOddWeek(d: LocalDate, sem: SemesterConfig): Boolean? {
        val start = sem.start ?: return null
        val odd = Math.floorMod(weekNumber(d, start), 2) == 1
        return odd != sem.invertParity
    }

    fun holidayLabel(d: LocalDate): String? {
        if (weekdayIndex(d) == 6) return "جمعه"
        val j = Jalali.fromLocalDate(d)
        if (j.month == 1 && j.day <= 7) return "تعطیلات نوروز"
        return null
    }

    fun isHoliday(d: LocalDate): Boolean = holidayLabel(d) != null

    fun inSemester(d: LocalDate, sem: SemesterConfig): Boolean {
        val s = sem.start
        val e = sem.end
        return (s == null || !d.isBefore(s)) && (e == null || !d.isAfter(e))
    }

    fun occursBase(c: ClassItem, d: LocalDate, sem: SemesterConfig): Boolean {
        if (c.weekday != weekdayIndex(d)) return false
        if (isHoliday(d)) return false
        if (!inSemester(d, sem)) return false
        if (c.pattern == 0) return true
        val odd = isOddWeek(d, sem) ?: return false
        return if (c.pattern == 1) odd else !odd
    }
}

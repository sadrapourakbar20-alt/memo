package com.sadra.memo.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

data class JDate(val year: Int, val month: Int, val day: Int)

fun String.toPersianDigits(): String =
    map { if (it in '0'..'9') '۰' + (it - '0') else it }.joinToString("")

object Jalali {
    val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)

    fun fromGregorian(gyIn: Int, gm: Int, gd: Int): JDate {
        var gy = gyIn
        var jy: Int
        if (gy > 1600) {
            jy = 979
            gy -= 1600
        } else {
            jy = 0
            gy -= 621
        }
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 - 80 + gd + gdm[gm - 1]
        jy += 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + days / 31
            jd = 1 + days % 31
        } else {
            jm = 7 + (days - 186) / 30
            jd = 1 + (days - 186) % 30
        }
        return JDate(jy, jm, jd)
    }

    fun toGregorian(j: JDate): LocalDate {
        var jy = j.year
        var gy: Int
        if (jy > 979) {
            gy = 1600
            jy -= 979
        } else {
            gy = 621
        }
        var days = 365 * jy + (jy / 33) * 8 + ((jy % 33) + 3) / 4 + 78 + j.day +
            (if (j.month < 7) (j.month - 1) * 31 else (j.month - 7) * 30 + 186)
        gy += 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            days -= 1
            gy += 100 * (days / 36524)
            days %= 36524
            if (days >= 365) days += 1
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val leap = (gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0
        val sal = intArrayOf(0, 31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 13 && gd > sal[gm]) {
            gd -= sal[gm]
            gm++
        }
        return LocalDate.of(gy, gm, gd)
    }

    fun fromLocalDate(d: LocalDate): JDate = fromGregorian(d.year, d.monthValue, d.dayOfMonth)

    fun today(): JDate = fromLocalDate(LocalDate.now())

    private fun isLeap(y: Int): Boolean {
        val g = toGregorian(JDate(y, 12, 30))
        return fromLocalDate(g) == JDate(y, 12, 30)
    }

    fun monthLength(y: Int, m: Int): Int = when {
        m <= 6 -> 31
        m <= 11 -> 30
        else -> if (isLeap(y)) 30 else 29
    }

    fun weekdayName(d: LocalDate): String = when (d.dayOfWeek) {
        DayOfWeek.SATURDAY -> "شنبه"
        DayOfWeek.SUNDAY -> "یکشنبه"
        DayOfWeek.MONDAY -> "دوشنبه"
        DayOfWeek.TUESDAY -> "سه‌شنبه"
        DayOfWeek.WEDNESDAY -> "چهارشنبه"
        DayOfWeek.THURSDAY -> "پنجشنبه"
        DayOfWeek.FRIDAY -> "جمعه"
        else -> ""
    }

    fun format(j: JDate): String = "${j.day} ${monthNames[j.month - 1]} ${j.year}".toPersianDigits()

    fun formatTime(hour: Int, minute: Int): String =
        String.format(Locale.US, "%02d:%02d", hour, minute).toPersianDigits()

    fun toMillis(j: JDate, hour: Int, minute: Int): Long =
        LocalDateTime.of(toGregorian(j), LocalTime.of(hour, minute))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun dateTimeOf(ms: Long): LocalDateTime =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDateTime()

    fun formatDueAt(ms: Long): String {
        val dt = dateTimeOf(ms)
        return format(fromLocalDate(dt.toLocalDate())) + " - " + formatTime(dt.hour, dt.minute)
    }

    fun startOfTodayMillis(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

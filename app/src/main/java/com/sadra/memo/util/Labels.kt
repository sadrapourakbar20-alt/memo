package com.sadra.memo.util

val weekdayLabels = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")

val patternLabels = listOf("هر هفته", "هفته‌های فرد", "هفته‌های زوج")

fun durationText(minutes: Int): String = when {
    minutes % 1440 == 0 -> "${minutes / 1440} روز"
    minutes % 60 == 0 -> "${minutes / 60} ساعت"
    else -> "$minutes دقیقه"
}.toPersianDigits()

fun offsetLabel(minutes: Int): String =
    if (minutes == 0) "سر ساعت" else durationText(minutes) + " قبل"

fun minutesText(totalMinutes: Int): String = Jalali.formatTime(totalMinutes / 60, totalMinutes % 60)

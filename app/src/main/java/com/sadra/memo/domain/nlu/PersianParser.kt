package com.sadra.memo.domain.nlu

import com.sadra.memo.util.JDate
import com.sadra.memo.util.Jalali
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class ParsedInput(val dueAt: Long?, val hasTime: Boolean, val title: String)

object PersianParser {
    private val numWords = mapOf(
        "یک" to 1, "یه" to 1, "دو" to 2, "سه" to 3, "چهار" to 4, "پنج" to 5, "شش" to 6,
        "هفت" to 7, "هشت" to 8, "نه" to 9, "ده" to 10, "بیست" to 20, "سی" to 30,
        "چهل" to 40, "پنجاه" to 50, "صد" to 100
    )
    private val weekdays = mapOf(
        "شنبه" to 0, "یکشنبه" to 1, "دوشنبه" to 2, "سه شنبه" to 3,
        "چهارشنبه" to 4, "پنجشنبه" to 5, "جمعه" to 6
    )

    private val t1 = Regex(
        """(?: (صبح|ظهر|عصر|شب|بعدازظهر|بعد از ظهر))? ساعت (\d{1,2})(?::(\d{2}))?(?: و (نیم|ربع|\d{1,2})(?: دقیقه)?)?(?: (صبح|ظهر|عصر|شب|بعدازظهر|بعد از ظهر))?(?= )"""
    )
    private val t2 = Regex(""" (\d{1,2}):(\d{2})(?: (صبح|ظهر|عصر|شب))?(?= )""")

    private val d1 = Regex(""" (\d{4})/(\d{1,2})/(\d{1,2})(?= )""")
    private val d2 = Regex(
        """ (\d{1,2}) (""" + Jalali.monthNames.joinToString("|") + """)(?: (\d{4}))?(?= )"""
    )
    private val d3 = Regex(""" (\d{1,3}|یک|یه|دو|سه|چهار|پنج|ده) (روز|هفته|ماه) (?:دیگه|دیگر|بعد)(?= )""")
    private val d4 = Regex(""" (هفته|ماه) (?:دیگه|دیگر|بعد|آینده|بعدی)(?= )""")
    private val d5 = Regex(""" (?:پس فردا|پسفردا)(?= )""")
    private val d6 = Regex(""" فردا(?= )""")
    private val d7 = Regex(""" امروز(?= )""")
    private val d8 = Regex(""" (یکشنبه|دوشنبه|سه شنبه|چهارشنبه|پنجشنبه|شنبه|جمعه)(?: (آینده|بعد|بعدی|این))?(?= )""")

    private val filler = Regex(
        """^(?:لطفا )?(?:برام )?(?:یادم بنداز|یادم باشه|یادآوری کن|یاد آوری کن|یادآوری|یاد آوری|یادداشت کن|یادداشت|ثبت کن|اضافه کن)(?: که)?\s+"""
    )
    private val trailingFiller = Regex("""\s+(?:رو |را )?(?:یادم بنداز|یادم باشه|یادآوری کن|یاد آوری کن)$""")
    private val leadingConnector = Regex("""^(?:که|برای|تا|در|به)\s+""")
    private val trailingConnector = Regex("""\s+(?:برای|تا|در|ساعت|که)$""")

    private fun resolveHour(h: Int, period: String?): Int {
        if (period == null) return if (h in 1..6) h + 12 else h
        return when (period.replace(" ", "")) {
            "صبح" -> if (h == 12) 0 else h
            "ظهر" -> if (h <= 2) h + 12 else h
            "عصر", "بعدازظهر" -> if (h < 12) h + 12 else h
            "شب" -> if (h == 12) 0 else if (h in 6..11) h + 12 else h
            else -> h
        }
    }

    fun addMonths(d: LocalDate, n: Int): LocalDate {
        val j = Jalali.fromLocalDate(d)
        val total = j.year * 12 + (j.month - 1) + n
        val y = total / 12
        val m = total % 12 + 1
        return Jalali.toGregorian(JDate(y, m, minOf(j.day, Jalali.monthLength(y, m))))
    }

    fun cleanTitle(raw: String): String {
        var t = raw.replace(Regex("""\s+"""), " ").trim()
        repeat(3) { t = filler.replaceFirst("$t ", "").trim() }
        t = trailingFiller.replace(t, "").trim()
        t = leadingConnector.replaceFirst(t, "").trim()
        t = trailingConnector.replace(t, "").trim()
        return t
    }

    fun parse(text: String, now: LocalDateTime = LocalDateTime.now()): ParsedInput {
        var t = TextNorm.normalize(text)
        val today = now.toLocalDate()
        var date: LocalDate? = null
        var time: Pair<Int, Int>? = null
        var hasTime = false

        fun cut(m: MatchResult) {
            t = t.substring(0, m.range.first) + " " + t.substring(m.range.last + 1)
        }

        val m1 = t1.find(t)
        if (m1 != null) {
            val g = m1.groupValues
            val h = g[2].toInt()
            var minute = if (g[3].isNotEmpty()) g[3].toInt() else 0
            if (g[4].isNotEmpty()) {
                minute = when (g[4]) {
                    "نیم" -> 30
                    "ربع" -> 15
                    else -> g[4].toInt()
                }
            }
            val period = g[1].ifEmpty { g[5] }.ifEmpty { null }
            val hh = resolveHour(h, period)
            if (hh < 24 && minute < 60) {
                time = hh to minute
                cut(m1)
            }
        }
        if (time == null) {
            val m2 = t2.find(t)
            if (m2 != null) {
                val g = m2.groupValues
                val raw = g[1].toInt()
                val hh = if (g[3].isNotEmpty()) resolveHour(raw, g[3]) else raw
                val minute = g[2].toInt()
                if (hh < 24 && minute < 60) {
                    time = hh to minute
                    cut(m2)
                }
            }
        }

        val patterns = listOf(d1, d2, d3, d4, d5, d6, d7, d8)
        for ((idx, pat) in patterns.withIndex()) {
            val m = pat.find(t) ?: continue
            val g = m.groupValues
            var found: LocalDate? = null
            when (idx) {
                0 -> {
                    val y = g[1].toInt()
                    val mo = g[2].toInt()
                    val d = g[3].toInt()
                    if (y in 1300..1600 && mo in 1..12 && d in 1..Jalali.monthLength(y, mo)) {
                        found = Jalali.toGregorian(JDate(y, mo, d))
                    }
                }
                1 -> {
                    val d = g[1].toInt()
                    val mo = Jalali.monthNames.indexOf(g[2]) + 1
                    val explicitYear = g[3].isNotEmpty()
                    val cy = Jalali.fromLocalDate(today).year
                    val y = if (explicitYear) g[3].toInt() else cy
                    if (mo in 1..12 && d >= 1) {
                        var cand = Jalali.toGregorian(JDate(y, mo, minOf(d, Jalali.monthLength(y, mo))))
                        if (!explicitYear && cand.isBefore(today)) {
                            cand = Jalali.toGregorian(
                                JDate(y + 1, mo, minOf(d, Jalali.monthLength(y + 1, mo)))
                            )
                        }
                        found = cand
                    }
                }
                2 -> {
                    val n = g[1].toIntOrNull() ?: (numWords[g[1]] ?: 0)
                    found = when (g[2]) {
                        "روز" -> today.plusDays(n.toLong())
                        "هفته" -> today.plusDays(7L * n)
                        else -> addMonths(today, n)
                    }
                }
                3 -> found = if (g[1] == "هفته") today.plusDays(7) else addMonths(today, 1)
                4 -> found = today.plusDays(2)
                5 -> found = today.plusDays(1)
                6 -> found = today
                else -> {
                    val target = weekdays[g[1]] ?: 0
                    val todayIdx = (today.dayOfWeek.value + 1) % 7
                    var delta = (target - todayIdx + 7) % 7
                    if (g[2] in listOf("آینده", "بعد", "بعدی") && delta == 0) delta = 7
                    found = today.plusDays(delta.toLong())
                }
            }
            if (found != null) {
                date = found
                cut(m)
                break
            }
        }

        if (date == null && time != null) {
            val cand = LocalDateTime.of(today, LocalTime.of(time.first, time.second))
            date = if (cand.isAfter(now)) today else today.plusDays(1)
        }

        var dueAt: Long? = null
        val finalDate = date
        if (finalDate != null) {
            val tm = time
            val (h, m) = if (tm == null) 9 to 0 else tm
            if (tm != null) hasTime = true
            dueAt = LocalDateTime.of(finalDate, LocalTime.of(h, m))
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        return ParsedInput(dueAt, hasTime, cleanTitle(t))
    }
}

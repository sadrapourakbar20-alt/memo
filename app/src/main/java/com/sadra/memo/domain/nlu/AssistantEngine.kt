package com.sadra.memo.domain.nlu

import java.time.LocalDateTime

enum class TaskFilter { TODAY, TOMORROW, WEEK, IMPORTANT_WEEK, OVERDUE, PENDING }

sealed interface AssistantIntent {
    data class RecordMoney(val info: MoneyInfo, val normalizedText: String) : AssistantIntent
    data class CreateNote(val content: String) : AssistantIntent
    data class CreateTask(val title: String, val dueAt: Long?) : AssistantIntent
    data class ShowTasks(val filter: TaskFilter) : AssistantIntent
    data class FindNote(val query: String) : AssistantIntent
    data class PlanDay(val offsetDays: Int) : AssistantIntent
    data object Balances : AssistantIntent
    data object MonthSummary : AssistantIntent
    data object Unknown : AssistantIntent
}

object AssistantEngine {
    private val noteTrigger = Regex("""^ (?:یادداشت کن|یادداشت بنویس|نوت کن)""")
    private val noteTriggerOriginal = Regex("""^\s*(?:یادداشت کن|یادداشت بنویس|نوت کن)\s*[:،-]?\s*""")
    private val monthSummary = Regex("""(?:خرج|هزینه|درآمد|واریزی).*(?:این ماه|ماه جاری)""")
    private val tasksStart = Regex("""^ (?:کارها|کارهای|کارای|کار های)""")
    private val stop = setOf(
        "یادداشت", "یادداشت‌ها", "یادداشتی", "مربوط", "به", "رو", "را", "پیدا", "کن", "جستجو",
        "بیاب", "سرچ", "درباره", "های", "بگرد", "دنبال", "بده", "نشون", "نشان", "کنید",
        "یه", "یک", "در", "از", "با", "برای"
    )

    fun classify(text: String, now: LocalDateTime = LocalDateTime.now()): AssistantIntent {
        val t = TextNorm.normalize(text)
        if (t.isBlank()) return AssistantIntent.Unknown

        val money = AmountParser.extract(text)
        if (money != null) return AssistantIntent.RecordMoney(money, t)

        if (noteTrigger.containsMatchIn(t)) {
            val content = noteTriggerOriginal.replaceFirst(text, "").trim()
            return AssistantIntent.CreateNote(content.ifBlank { text.trim() })
        }
        if (t.contains("موجودی")) return AssistantIntent.Balances
        if (monthSummary.containsMatchIn(t)) return AssistantIntent.MonthSummary

        if (listOf("پیدا کن", "جستجو", "بیاب", "سرچ", "بگرد").any { t.contains(it) }) {
            val words = t.trim().split(" ").filter { it.isNotBlank() && it !in stop }
            return AssistantIntent.FindNote(words.joinToString(" "))
        }

        val asksList = listOf("نشون", "نشان", "لیست", "چه ", "چیا", "کدوم", "چند", "بده", "مونده").any { t.contains(it) }
        if (t.contains("کار") && (asksList || tasksStart.containsMatchIn(t))) {
            val f = when {
                t.contains("عقب") -> TaskFilter.OVERDUE
                listOf("ندادم", "نشده", "مونده", "باقی", "نکردم").any { t.contains(it) } -> TaskFilter.PENDING
                t.contains("مهم") && t.contains("هفته") -> TaskFilter.IMPORTANT_WEEK
                t.contains("هفته") -> TaskFilter.WEEK
                t.contains("امروز") -> TaskFilter.TODAY
                t.contains("فردا") -> TaskFilter.TOMORROW
                else -> TaskFilter.PENDING
            }
            return AssistantIntent.ShowTasks(f)
        }

        if (t.contains("برنامه") && listOf("امروز", "فردا", "روزانه", "پیشنهاد").any { t.contains(it) }) {
            return AssistantIntent.PlanDay(if (t.contains("فردا")) 1 else 0)
        }

        val trigger = listOf("یادم بنداز", "یادم باشه", "یادآوری", "یاد آوری", "کار جدید", "وظیفه", "تسک")
            .any { t.contains(it) }
        val parsed = PersianParser.parse(text, now)
        if (trigger || parsed.dueAt != null) {
            return AssistantIntent.CreateTask(parsed.title.ifBlank { text.trim() }, parsed.dueAt)
        }
        return AssistantIntent.Unknown
    }
}

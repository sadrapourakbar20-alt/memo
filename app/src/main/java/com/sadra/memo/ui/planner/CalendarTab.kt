package com.sadra.memo.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.sadra.memo.domain.model.ClassItem
import com.sadra.memo.domain.schedule.ClassSchedule
import com.sadra.memo.util.JDate
import com.sadra.memo.util.Jalali
import com.sadra.memo.util.minutesText
import com.sadra.memo.util.patternLabels
import com.sadra.memo.util.toPersianDigits
import com.sadra.memo.util.weekdayLabels
import java.time.LocalDate

private val shortWeekdays = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

@Composable
fun CalendarTab(vm: ScheduleViewModel, onOpenTask: (Long) -> Unit) {
    val classes by vm.classes.collectAsState()
    val cancelled by vm.cancellations.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val sem by vm.semester.collectAsState()

    var selectedEpoch by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var mode by rememberSaveable { mutableStateOf(2) } // 0 روز، 1 هفته، 2 ماه

    val today = LocalDate.now()
    val selected = LocalDate.ofEpochDay(selectedEpoch)
    val j = Jalali.fromLocalDate(selected)

    val tasksByDate = remember(tasks) {
        tasks.filter { it.dueAt != null }
            .groupBy { Jalali.dateTimeOf(it.dueAt ?: 0L).toLocalDate() }
    }

    fun classesOn(date: LocalDate): List<ClassItem> =
        classes.filter { ClassSchedule.occursBase(it, date, sem) }.sortedBy { it.startMinutes }

    fun hasItems(date: LocalDate): Boolean {
        if (tasksByDate[date]?.isNotEmpty() == true) return true
        return classesOn(date).any { (it.id to date.toEpochDay()) !in cancelled }
    }

    fun move(dir: Int) {
        val next: LocalDate = when (mode) {
            0 -> selected.plusDays(dir.toLong())
            1 -> selected.plusWeeks(dir.toLong())
            else -> {
                var m = j.month + dir
                var y = j.year
                if (m > 12) { m = 1; y += 1 }
                if (m < 1) { m = 12; y -= 1 }
                val d = minOf(j.day, Jalali.monthLength(y, m))
                Jalali.toGregorian(JDate(y, m, d))
            }
        }
        selectedEpoch = next.toEpochDay()
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("روز", "هفته", "ماه").forEachIndexed { i, label ->
                FilterChip(selected = mode == i, onClick = { mode = i }, label = { Text(label) })
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { move(-1) }) { Text("قبلی") }
            Text(
                text = if (mode == 0) Jalali.format(j)
                else (Jalali.monthNames[j.month - 1] + " " + j.year).toPersianDigits(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { move(1) }) { Text("بعدی") }
            TextButton(onClick = { selectedEpoch = today.toEpochDay() }) { Text("امروز") }
        }

        val weekInfo = when {
            sem.start == null -> "برای دیدن هفته فرد و زوج، شروع ترم رو تو تب «برنامه کلاسی» تنظیم کن."
            !ClassSchedule.inSemester(selected, sem) -> "این روز خارج از ترمه."
            else -> {
                val n = ClassSchedule.weekNumber(selected, sem.start ?: selected)
                val odd = ClassSchedule.isOddWeek(selected, sem) == true
                ("هفته " + n + " ترم - " + (if (odd) "فرد" else "زوج")).toPersianDigits()
            }
        }
        Text(
            text = weekInfo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (mode != 0) {
            Row(modifier = Modifier.fillMaxWidth()) {
                shortWeekdays.forEachIndexed { i, label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (i == 6) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (mode == 2) {
            val first = Jalali.toGregorian(JDate(j.year, j.month, 1))
            val lead = ClassSchedule.weekdayIndex(first)
            val len = Jalali.monthLength(j.year, j.month)
            val rows = (lead + len + 6) / 7
            for (r in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val dayNum = r * 7 + c - lead + 1
                        if (dayNum in 1..len) {
                            val date = first.plusDays((dayNum - 1).toLong())
                            DayCell(
                                label = dayNum.toString().toPersianDigits(),
                                selected = date == selected,
                                isToday = date == today,
                                holiday = ClassSchedule.isHoliday(date),
                                hasItems = hasItems(date),
                                onClick = { selectedEpoch = date.toEpochDay() },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else if (mode == 1) {
            val start = ClassSchedule.weekStart(selected)
            Row(modifier = Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val date = start.plusDays(c.toLong())
                    DayCell(
                        label = Jalali.fromLocalDate(date).day.toString().toPersianDigits(),
                        selected = date == selected,
                        isToday = date == today,
                        holiday = ClassSchedule.isHoliday(date),
                        hasItems = hasItems(date),
                        onClick = { selectedEpoch = date.toEpochDay() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ---- برنامه روز انتخاب‌شده ----
        Text(
            text = Jalali.weekdayName(selected) + " " + Jalali.format(Jalali.fromLocalDate(selected)),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        val holiday = ClassSchedule.holidayLabel(selected)
        if (holiday != null) {
            Text(
                text = "تعطیل: $holiday",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        val dayClasses = classesOn(selected)
        val dayTasks = (tasksByDate[selected] ?: emptyList()).sortedBy { it.dueAt }

        dayClasses.forEach { c ->
            val isCancelled = (c.id to selected.toEpochDay()) in cancelled
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = c.name,
                            style = MaterialTheme.typography.titleMedium,
                            textDecoration = if (isCancelled) TextDecoration.LineThrough else null
                        )
                        Text(
                            text = minutesText(c.startMinutes) + " تا " + minutesText(c.endMinutes) +
                                (if (c.location.isNotBlank()) " · " + c.location else "") +
                                (if (c.teacher.isNotBlank()) " · " + c.teacher else ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isCancelled) {
                            Text(
                                "این جلسه برگزار نمی‌شه",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    TextButton(onClick = { vm.toggleCancel(c.id, selected) }) {
                        Text(if (isCancelled) "فعال‌سازی" else "لغو جلسه")
                    }
                }
            }
        }

        dayTasks.forEach { t ->
            Card(
                onClick = { onOpenTask(t.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = t.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (t.isDone) TextDecoration.LineThrough else null
                    )
                    val due = t.dueAt
                    if (due != null) {
                        val dt = Jalali.dateTimeOf(due)
                        Text(
                            text = Jalali.formatTime(dt.hour, dt.minute),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (dayClasses.isEmpty() && dayTasks.isEmpty()) {
            Text(
                "برنامه‌ای برای این روز نیست.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DayCell(
    label: String,
    selected: Boolean,
    isToday: Boolean,
    holiday: Boolean,
    hasItems: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    else MaterialTheme.colorScheme.surface.copy(alpha = 0f)
    Box(
        modifier = modifier
            .padding(2.dp)
            .height(46.dp)
            .background(bg, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    holiday -> MaterialTheme.colorScheme.error
                    isToday -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(5.dp)
                    .background(
                        if (hasItems) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                        CircleShape
                    )
            )
        }
    }
}

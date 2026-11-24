package com.sadra.memo.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sadra.memo.MemoApplication
import com.sadra.memo.ui.factoryOf
import com.sadra.memo.util.Jalali
import com.sadra.memo.util.toPersianDigits
import java.time.LocalDateTime

@Composable
fun HomeScreen(onOpenNote: (Long) -> Unit, onOpenTask: (Long) -> Unit) {
    val app = LocalContext.current.applicationContext as MemoApplication
    val vm: HomeViewModel = viewModel(
        factory = remember {
            factoryOf { HomeViewModel(app.container.noteRepository, app.container.taskRepository) }
        }
    )
    val state by vm.state.collectAsState()

    val nowDt = LocalDateTime.now()
    val today = nowDt.toLocalDate()
    val greeting = when (nowDt.hour) {
        in 5..11 -> "صبح بخیر"
        in 12..16 -> "ظهر بخیر"
        in 17..20 -> "عصر بخیر"
        else -> "شب بخیر"
    }
    val nowMs = System.currentTimeMillis()
    val startToday = Jalali.startOfTodayMillis()
    val endToday = startToday + 24L * 60 * 60 * 1000

    val todayTasks = state.tasks.filter { val d = it.dueAt; d != null && d >= startToday && d < endToday }
    val overdue = state.tasks.filter { val d = it.dueAt; !it.isDone && d != null && d < nowMs }
    val upcoming = state.tasks
        .filter { val d = it.dueAt; !it.isDone && d != null && d >= nowMs }
        .sortedBy { it.dueAt }
        .take(3)
    val progress = if (todayTasks.isEmpty()) 0f
    else todayTasks.count { it.isDone }.toFloat() / todayTasks.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text(greeting, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = Jalali.weekdayName(today) + " " + Jalali.format(Jalali.fromLocalDate(today)) +
                    " - " + Jalali.formatTime(nowDt.hour, nowDt.minute),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("کارهای امروز", todayTasks.size.toString().toPersianDigits(), Modifier.weight(1f))
            StatCard(
                "عقب‌افتاده",
                overdue.size.toString().toPersianDigits(),
                Modifier.weight(1f),
                highlight = overdue.isNotEmpty()
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("پیشرفت امروز", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (todayTasks.isEmpty()) "برای امروز کاری ثبت نشده."
                    else (todayTasks.count { it.isDone }.toString() + " از " + todayTasks.size.toString() + " کار انجام شده").toPersianDigits(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
        }

        if (upcoming.isNotEmpty()) {
            Text("یادآوری‌های نزدیک", style = MaterialTheme.typography.titleMedium)
            upcoming.forEach { task ->
                Card(
                    onClick = { onOpenTask(task.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(task.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val due = task.dueAt
                        if (due != null) {
                            Text(
                                Jalali.formatDueAt(due),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (state.recentNotes.isNotEmpty()) {
            Text("یادداشت‌های اخیر", style = MaterialTheme.typography.titleMedium)
            state.recentNotes.forEach { note ->
                Card(
                    onClick = { onOpenNote(note.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val title = note.title.ifBlank { note.content.lineSequence().firstOrNull().orEmpty() }
                        Text(title.ifBlank { "بدون عنوان" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (note.title.isNotBlank() && note.content.isNotBlank()) {
                            Text(
                                note.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier, highlight: Boolean = false) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

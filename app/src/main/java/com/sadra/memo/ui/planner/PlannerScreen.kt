package com.sadra.memo.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sadra.memo.MemoApplication
import com.sadra.memo.domain.model.Task
import com.sadra.memo.ui.factoryOf
import com.sadra.memo.util.Jalali

@Composable
fun PlannerScreen(onOpenTask: (Long) -> Unit) {
    val app = LocalContext.current.applicationContext as MemoApplication
    val vm: PlannerViewModel = viewModel(
        factory = remember {
            factoryOf { PlannerViewModel(app.container.taskRepository, app.container.reminderScheduler) }
        }
    )
    val tasks by vm.tasks.collectAsState()

    val now = System.currentTimeMillis()
    val startToday = Jalali.startOfTodayMillis()
    val endToday = startToday + 24L * 60 * 60 * 1000

    val overdue = tasks.filter { val d = it.dueAt; !it.isDone && d != null && d < now }
    val today = tasks.filter { val d = it.dueAt; !it.isDone && d != null && d >= now && d < endToday }
    val later = tasks.filter { val d = it.dueAt; !it.isDone && (d == null || d >= endToday) }
    val done = tasks.filter { it.isDone }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        val todayDate = java.time.LocalDate.now()
        Text(
            text = "برنامه‌ریز",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = Jalali.weekdayName(todayDate) + " " + Jalali.format(Jalali.fromLocalDate(todayDate)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
        )
        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "هنوز کاری ثبت نکردی. با دکمه + یه کار بساز.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                section("عقب‌افتاده", overdue, vm, onOpenTask, overdueStyle = true)
                section("امروز", today, vm, onOpenTask)
                section("بعداً و بدون تاریخ", later, vm, onOpenTask)
                section("انجام‌شده", done, vm, onOpenTask)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    list: List<Task>,
    vm: PlannerViewModel,
    onOpenTask: (Long) -> Unit,
    overdueStyle: Boolean = false
) {
    if (list.isEmpty()) return
    item(key = "header_$title") {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = if (overdueStyle) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
    items(list, key = { it.id }) { task ->
        TaskRow(task, onClick = { onOpenTask(task.id) }, onToggle = { vm.toggleDone(task) })
    }
}

@Composable
private fun TaskRow(task: Task, onClick: () -> Unit, onToggle: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.isDone, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val due = task.dueAt
                if (due != null) {
                    Text(
                        text = Jalali.formatDueAt(due),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val dot: Color = when (task.priority) {
                2 -> MaterialTheme.colorScheme.error
                1 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            Box(modifier = Modifier.padding(end = 12.dp).size(10.dp).background(dot, CircleShape))
        }
    }
}

package com.sadra.memo.ui.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sadra.memo.MemoApplication
import com.sadra.memo.ui.factoryOf

@Composable
fun PlannerScreen(onOpenTask: (Long) -> Unit, onOpenClass: (Long) -> Unit) {
    val app = LocalContext.current.applicationContext as MemoApplication
    val scheduleVm: ScheduleViewModel = viewModel(
        factory = remember {
            factoryOf {
                ScheduleViewModel(
                    app.container.classRepository,
                    app.container.taskRepository,
                    app.container.settingsRepository,
                    app.container.classScheduler
                )
            }
        }
    )
    var tab by rememberSaveable { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            text = "برنامه‌ریز",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("کارها", "تقویم", "برنامه کلاسی").forEachIndexed { i, label ->
                FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(label) })
            }
        }
        when (tab) {
            0 -> TasksTab(onOpenTask)
            1 -> CalendarTab(scheduleVm, onOpenTask)
            else -> ClassesTab(scheduleVm, onOpenClass)
        }
    }
}

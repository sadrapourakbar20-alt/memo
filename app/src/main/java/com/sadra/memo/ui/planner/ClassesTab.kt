package com.sadra.memo.ui.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sadra.memo.domain.schedule.ClassSchedule
import com.sadra.memo.ui.components.JalaliDatePickerDialog
import com.sadra.memo.util.Jalali
import com.sadra.memo.util.minutesText
import com.sadra.memo.util.patternLabels
import com.sadra.memo.util.toPersianDigits
import com.sadra.memo.util.weekdayLabels
import java.time.LocalDate

@Composable
fun ClassesTab(vm: ScheduleViewModel, onOpenClass: (Long) -> Unit) {
    val classes by vm.classes.collectAsState()
    val sem by vm.semester.collectAsState()
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("تنظیمات ترم", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { pickStart = true }) {
                        Text(sem.start?.let { Jalali.format(Jalali.fromLocalDate(it)) } ?: "شروع ترم")
                    }
                    if (sem.start != null) TextButton(onClick = { vm.setStart(null) }) { Text("پاک") }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { pickEnd = true }) {
                        Text(sem.end?.let { Jalali.format(Jalali.fromLocalDate(it)) } ?: "پایان ترم")
                    }
                    if (sem.end != null) TextButton(onClick = { vm.setEnd(null) }) { Text("پاک") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("هفته اول ترم زوج حساب بشه", modifier = Modifier.weight(1f))
                    Switch(checked = sem.invertParity, onCheckedChange = { vm.setInvert(it) })
                }
                val today = LocalDate.now()
                val info = if (sem.start == null) {
                    "شروع ترم رو بذار تا هفته فرد و زوج حساب بشه. هفته‌ای که شروع ترم توش هست، هفته اول (فرد) حساب می‌شه."
                } else if (!ClassSchedule.inSemester(today, sem)) {
                    "امروز خارج از ترمه."
                } else {
                    val n = ClassSchedule.weekNumber(today, sem.start ?: today)
                    val odd = ClassSchedule.isOddWeek(today, sem) == true
                    ("الان هفته " + n + " ترمه (" + (if (odd) "فرد" else "زوج") + ")").toPersianDigits()
                }
                Text(
                    text = info,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "جمعه‌ها و ۷ روز اول فروردین تعطیله و برای کلاس‌ها یادآوری نمیاد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Button(onClick = { onOpenClass(0L) }) { Text("کلاس جدید") }

        if (classes.any { it.pattern != 0 } && sem.start == null) {
            Text(
                "کلاس‌های هفته فرد و زوج بدون شروع ترم نمایش داده نمی‌شن.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (classes.isEmpty()) {
            Text(
                "هنوز کلاسی ثبت نکردی.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        classes.forEach { c ->
            Card(
                onClick = { onOpenClass(c.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(c.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = weekdayLabels[c.weekday] + " " + minutesText(c.startMinutes) + " تا " +
                            minutesText(c.endMinutes) + " · " + patternLabels[c.pattern],
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val extra = listOf(c.location, c.teacher).filter { it.isNotBlank() }.joinToString(" · ")
                    if (extra.isNotEmpty()) {
                        Text(
                            extra,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (pickStart) {
        JalaliDatePickerDialog(
            initial = sem.start?.let { Jalali.fromLocalDate(it) } ?: Jalali.today(),
            onDismiss = { pickStart = false },
            onConfirm = { vm.setStart(Jalali.toGregorian(it)); pickStart = false }
        )
    }
    if (pickEnd) {
        JalaliDatePickerDialog(
            initial = sem.end?.let { Jalali.fromLocalDate(it) } ?: Jalali.today(),
            onDismiss = { pickEnd = false },
            onConfirm = { vm.setEnd(Jalali.toGregorian(it)); pickEnd = false }
        )
    }
}

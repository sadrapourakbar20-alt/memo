package com.sadra.memo.ui.planner

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sadra.memo.MemoApplication
import com.sadra.memo.ui.factoryOf
import com.sadra.memo.util.minutesText
import com.sadra.memo.util.offsetLabel
import com.sadra.memo.util.patternLabels
import com.sadra.memo.util.weekdayLabels

private val classOffsetOptions = listOf(0, 10, 30, 60)

@Composable
fun ClassEditScreen(classId: Long, onClose: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as MemoApplication
    val vm: ClassEditViewModel = viewModel(
        factory = remember(classId) {
            factoryOf {
                ClassEditViewModel(app.container.classRepository, app.container.classScheduler, classId)
            }
        }
    )
    val state by vm.state.collectAsState()
    var showDelete by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { vm.saveAndClose(onClose) }

    val save: () -> Unit = {
        val needsPermission = state.offsets.isNotEmpty() &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else vm.saveAndClose(onClose)
    }

    BackHandler { onClose() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت بدون ذخیره")
            }
            Text(
                text = if (classId > 0) "ویرایش کلاس" else "کلاس جدید",
                modifier = Modifier.weight(1f)
            )
            if (classId > 0) {
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "حذف")
                }
            }
            IconButton(onClick = save) {
                Icon(Icons.Filled.Check, contentDescription = "ذخیره")
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = vm::onName,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("اسم درس") },
                singleLine = true
            )
            OutlinedTextField(
                value = state.teacher,
                onValueChange = vm::onTeacher,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                label = { Text("استاد (اختیاری)") },
                singleLine = true
            )
            OutlinedTextField(
                value = state.location,
                onValueChange = vm::onLocation,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                label = { Text("محل کلاس (اختیاری)") },
                singleLine = true
            )

            Text("روز هفته", modifier = Modifier.padding(top = 16.dp))
            LazyRow(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items((0..5).toList()) { d ->
                    FilterChip(
                        selected = state.weekday == d,
                        onClick = { vm.onWeekday(d) },
                        label = { Text(weekdayLabels[d]) }
                    )
                }
            }

            Text("ساعت کلاس", modifier = Modifier.padding(top = 16.dp))
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    TimePickerDialog(
                        context,
                        { _, h, m -> vm.onStart(h, m) },
                        state.start / 60,
                        state.start % 60,
                        true
                    ).show()
                }) { Text("شروع: " + minutesText(state.start)) }
                OutlinedButton(onClick = {
                    TimePickerDialog(
                        context,
                        { _, h, m -> vm.onEnd(h, m) },
                        state.end / 60,
                        state.end % 60,
                        true
                    ).show()
                }) { Text("پایان: " + minutesText(state.end)) }
            }

            Text("تکرار", modifier = Modifier.padding(top = 16.dp))
            LazyRow(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items((0..2).toList()) { p ->
                    FilterChip(
                        selected = state.pattern == p,
                        onClick = { vm.onPattern(p) },
                        label = { Text(patternLabels[p]) }
                    )
                }
            }
            Text(
                text = "فرد و زوج از روی «شروع ترم» حساب می‌شه که تو تب برنامه کلاسی تنظیم می‌کنی.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )

            Text("یادآوری قبل از کلاس", modifier = Modifier.padding(top = 16.dp))
            LazyRow(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(classOffsetOptions) { off ->
                    FilterChip(
                        selected = off in state.offsets,
                        onClick = { vm.toggleOffset(off) },
                        label = { Text(offsetLabel(off)) }
                    )
                }
            }

            val error = state.error
            if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("حذف کلاس") },
            text = { Text("این کلاس و همه جلسه‌های لغوشده‌ش پاک می‌شه.") },
            confirmButton = {
                TextButton(onClick = { showDelete = false; vm.deleteAndClose(onClose) }) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("انصراف") }
            }
        )
    }
}

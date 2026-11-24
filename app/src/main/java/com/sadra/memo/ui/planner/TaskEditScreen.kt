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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Slider
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
import com.sadra.memo.ui.components.JalaliDatePickerDialog
import com.sadra.memo.ui.factoryOf
import com.sadra.memo.util.Jalali
import com.sadra.memo.util.toPersianDigits

@Composable
fun TaskEditScreen(taskId: Long, onClose: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as MemoApplication
    val vm: TaskEditViewModel = viewModel(
        factory = remember(taskId) {
            factoryOf {
                TaskEditViewModel(app.container.taskRepository, app.container.reminderScheduler, taskId)
            }
        }
    )
    val state by vm.state.collectAsState()
    var showDelete by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { vm.saveAndClose(onClose) }

    val save: () -> Unit = {
        val needsPermission = state.date != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else vm.saveAndClose(onClose)
    }

    BackHandler { save() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = save) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ذخیره و بازگشت")
            }
            Text(
                text = if (taskId > 0) "ویرایش کار" else "کار جدید",
                modifier = Modifier.weight(1f)
            )
            if (taskId > 0) {
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
                value = state.title,
                onValueChange = vm::onTitle,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("عنوان کار") },
                singleLine = true
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = vm::onDescription,
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp).padding(top = 12.dp),
                label = { Text("توضیحات") }
            )

            Text("اولویت", modifier = Modifier.padding(top = 16.dp))
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0 to "کم", 1 to "متوسط", 2 to "زیاد").forEach { (value, label) ->
                    FilterChip(
                        selected = state.priority == value,
                        onClick = { vm.onPriority(value) },
                        label = { Text(label) }
                    )
                }
            }

            Text("زمان انجام و یادآوری", modifier = Modifier.padding(top = 16.dp))
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { showDate = true }) {
                    Text(state.date?.let { Jalali.format(it) } ?: "انتخاب تاریخ")
                }
                if (state.date != null) {
                    OutlinedButton(onClick = {
                        TimePickerDialog(
                            context,
                            { _, h, m -> vm.onTime(h, m) },
                            state.hour,
                            state.minute,
                            true
                        ).show()
                    }) {
                        Text(Jalali.formatTime(state.hour, state.minute))
                    }
                    TextButton(onClick = { vm.onDate(null) }) { Text("حذف زمان") }
                }
            }
            if (state.date != null) {
                Text(
                    text = "در همین زمان برات اعلان میاد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Text(
                text = "پیشرفت: " + (state.progress.toString() + "٪").toPersianDigits(),
                modifier = Modifier.padding(top = 16.dp)
            )
            Slider(
                value = state.progress.toFloat(),
                onValueChange = { vm.onProgress(it.toInt()) },
                valueRange = 0f..100f
            )
        }
    }

    if (showDate) {
        JalaliDatePickerDialog(
            initial = state.date ?: Jalali.today(),
            onDismiss = { showDate = false },
            onConfirm = { vm.onDate(it); showDate = false }
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("حذف کار") },
            text = { Text("این کار برای همیشه پاک می‌شه. مطمئنی؟") },
            confirmButton = {
                TextButton(onClick = { showDelete = false; vm.deleteAndClose(onClose) }) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("انصراف") }
            }
        )
    }
}

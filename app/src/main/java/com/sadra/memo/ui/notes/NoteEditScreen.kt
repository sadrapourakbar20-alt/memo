package com.sadra.memo.ui.notes

import androidx.activity.compose.BackHandler
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sadra.memo.MemoApplication
import com.sadra.memo.ui.factoryOf

@Composable
fun NoteEditScreen(noteId: Long, onClose: () -> Unit) {
    val app = LocalContext.current.applicationContext as MemoApplication
    val vm: NoteEditViewModel = viewModel(
        factory = remember(noteId) {
            factoryOf { NoteEditViewModel(app.container.noteRepository, noteId) }
        }
    )
    val state by vm.state.collectAsState()
    var showDelete by remember { mutableStateOf(false) }

    BackHandler { vm.saveAndClose(onClose) }

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
            IconButton(onClick = { vm.saveAndClose(onClose) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ذخیره و بازگشت")
            }
            Text(
                text = if (noteId > 0) "ویرایش یادداشت" else "یادداشت جدید",
                modifier = Modifier.weight(1f)
            )
            if (noteId > 0) {
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "حذف")
                }
            }
            IconButton(onClick = { vm.saveAndClose(onClose) }) {
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
                label = { Text("عنوان") },
                singleLine = true
            )
            OutlinedTextField(
                value = state.category,
                onValueChange = vm::onCategory,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                label = { Text("دسته‌بندی") },
                singleLine = true
            )
            FilterChip(
                selected = state.pinned,
                onClick = { vm.onPinned(!state.pinned) },
                label = { Text("سنجاق‌شده") },
                modifier = Modifier.padding(top = 12.dp)
            )
            OutlinedTextField(
                value = state.content,
                onValueChange = vm::onContent,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp)
                    .padding(top = 12.dp),
                label = { Text("متن یادداشت") }
            )
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("انتقال به سطل زباله") },
            text = { Text("یادداشت به سطل زباله می‌ره و می‌تونی از اونجا برش گردونی.") },
            confirmButton = {
                TextButton(onClick = { showDelete = false; vm.deleteAndClose(onClose) }) {
                    Text("انتقال")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("انصراف") }
            }
        )
    }
}

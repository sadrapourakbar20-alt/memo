package com.sadra.memo.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sadra.memo.MemoApplication
import com.sadra.memo.domain.model.Note
import com.sadra.memo.ui.factoryOf

@Composable
fun TrashScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as MemoApplication
    val vm: TrashViewModel = viewModel(
        factory = remember { factoryOf { TrashViewModel(app.container.noteRepository, app.container.attachmentManager) } }
    )
    val notes by vm.notes.collectAsState()
    var pendingDelete by remember { mutableStateOf<Note?>(null) }
    var confirmEmpty by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
            }
            Text(
                text = "سطل زباله",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            if (notes.isNotEmpty()) {
                TextButton(onClick = { confirmEmpty = true }) { Text("خالی کردن") }
            }
        }
        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "سطل زباله خالیه.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val title = note.title.ifBlank { note.content.lineSequence().firstOrNull().orEmpty() }
                            Text(
                                text = title.ifBlank { "بدون عنوان" },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (note.title.isNotBlank() && note.content.isNotBlank()) {
                                Text(
                                    text = note.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Row(modifier = Modifier.padding(top = 8.dp)) {
                                TextButton(onClick = { vm.restore(note.id) }) { Text("بازیابی") }
                                TextButton(onClick = { pendingDelete = note }) { Text("حذف دائمی") }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("حذف دائمی") },
            text = { Text("این یادداشت برای همیشه پاک می‌شه و برنمی‌گرده.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteForever(note.id); pendingDelete = null }) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("انصراف") }
            }
        )
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            title = { Text("خالی کردن سطل زباله") },
            text = { Text("همه یادداشت‌های داخل سطل برای همیشه پاک می‌شن.") },
            confirmButton = {
                TextButton(onClick = { vm.emptyTrash(); confirmEmpty = false }) { Text("خالی کن") }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmpty = false }) { Text("انصراف") }
            }
        )
    }
}

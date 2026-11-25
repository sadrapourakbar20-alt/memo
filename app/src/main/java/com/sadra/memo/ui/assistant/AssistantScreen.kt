package com.sadra.memo.ui.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.sadra.memo.ui.components.SpeechButton
import com.sadra.memo.ui.factoryOf

private val examples = listOf(
    "کارهای امروز",
    "برنامه امروز",
    "فردا ساعت ۱۰ صبح تماس با مشتری",
    "موجودی حساب‌ها",
    "چه کارهایی رو انجام ندادم؟"
)

@Composable
fun AssistantScreen(onOpenNote: (Long) -> Unit, onOpenTask: (Long) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as MemoApplication
    val vm: AssistantViewModel = viewModel(
        factory = remember {
            factoryOf {
                AssistantViewModel(
                    app.container.noteRepository,
                    app.container.taskRepository,
                    app.container.taskManager,
                    app.container.classRepository,
                    app.container.financeRepository,
                    app.container.settingsRepository
                )
            }
        }
    )
    val messages by vm.messages.collectAsState()
    val askPermission by vm.askPermission.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(askPermission) {
        if (askPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            vm.permissionHandled()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 12.dp)) {
        Text(
            "دستیار",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(messages, key = { it.id }) { m ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (m.fromUser) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.88f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (m.fromUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(m.text)
                            if (m.items.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    m.items.forEach { item ->
                                        AssistChip(
                                            onClick = { if (item.isTask) onOpenTask(item.id) else onOpenNote(item.id) },
                                            label = { Text(item.label) }
                                        )
                                    }
                                }
                            }
                            if (m.choices.isNotEmpty()) {
                                LazyRow(
                                    modifier = Modifier.padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(m.choices) { c ->
                                        AssistChip(onClick = { vm.chooseAccount(c) }, label = { Text(c.label) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (messages.size <= 1) {
            LazyRow(
                modifier = Modifier.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(examples) { ex ->
                    AssistChip(onClick = { vm.send(ex) }, label = { Text(ex) })
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("بنویس یا بگو...") },
                maxLines = 3
            )
            SpeechButton(label = "گفتار") { spoken ->
                input = spoken
                vm.send(spoken)
                input = ""
            }
            TextButton(onClick = { vm.send(input); input = "" }) { Text("ارسال") }
        }
    }
}

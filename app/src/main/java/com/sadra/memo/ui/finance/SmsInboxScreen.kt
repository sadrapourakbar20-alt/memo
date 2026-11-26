package com.sadra.memo.ui.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import com.sadra.memo.ui.factoryOf
import com.sadra.memo.util.Jalali
import com.sadra.memo.util.formatMoney

@Composable
fun SmsInboxScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as MemoApplication
    val vm: SmsInboxViewModel = viewModel(
        factory = remember {
            factoryOf { SmsInboxViewModel(app.container.smsInboxRepository, app.container.financeRepository) }
        }
    )
    val items by vm.items.collectAsState()
    val accounts by vm.accounts.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
            }
            Text("پیامک‌های منتظر تأیید", style = MaterialTheme.typography.titleLarge)
        }
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("پیامکی منتظر نیست.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    var rememberSender by remember(item.id) { mutableStateOf(true) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = (if (item.type == 0) "برداشت " else "واریز ") + formatMoney(item.amount),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (item.type == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                            )
                            val dt = Jalali.dateTimeOf(item.dateMillis)
                            Text(
                                text = Jalali.format(Jalali.fromLocalDate(dt.toLocalDate())) + " - " +
                                    Jalali.formatTime(dt.hour, dt.minute) + " · از: " + item.sender,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = item.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            if (item.sender.isNotBlank() && accounts.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = rememberSender,
                                        onCheckedChange = { rememberSender = it }
                                    )
                                    Text(
                                        text = "سرشماره «${item.sender}» رو به این حساب وصل کن",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Text(
                                text = if (item.type == 0) "از کدوم حساب؟" else "به کدوم حساب؟",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            if (accounts.isEmpty()) {
                                Text("اول تو تب مالی یه حساب اضافه کن.", color = MaterialTheme.colorScheme.error)
                            } else {
                                LazyRow(
                                    modifier = Modifier.padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(accounts, key = { it.id }) { a ->
                                        AssistChip(
                                            onClick = { vm.assign(item, a.id, rememberSender) },
                                            label = { Text(a.name) }
                                        )
                                    }
                                }
                            }
                            TextButton(onClick = { vm.ignore(item) }) { Text("نادیده بگیر (تراکنش نیست)") }
                        }
                    }
                }
            }
        }
    }
}

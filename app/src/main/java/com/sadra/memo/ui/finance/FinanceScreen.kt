package com.sadra.memo.ui.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sadra.memo.MemoApplication
import com.sadra.memo.ui.factoryOf
import com.sadra.memo.util.Jalali
import com.sadra.memo.util.accountTypeLabels
import com.sadra.memo.util.formatMoney

@Composable
fun FinanceScreen(
    onAddAccount: () -> Unit,
    onOpenAccount: (Long) -> Unit,
    onAddTx: (Int) -> Unit,
    onOpenTx: (Long) -> Unit
) {
    val app = LocalContext.current.applicationContext as MemoApplication
    val vm: FinanceViewModel = viewModel(
        factory = remember { factoryOf { FinanceViewModel(app.container.financeRepository) } }
    )
    val state by vm.state.collectAsState()
    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("مالی", style = MaterialTheme.typography.headlineSmall)

        if (state.loaded && state.accounts.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = cardColors) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("اول حساب‌هات رو وارد کن", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "هر حسابی که پول توش داری یا ازش خرج می‌کنی (مثلاً بانک ملی، بانک ملت، کیف پول نقدی) رو با موجودی فعلیش اضافه کن. بعد از اون، هر خرج یا واریزی رو که ثبت کنی، ازت می‌پرسم مال کدوم حسابه و تو همون حساب ثبت می‌شه.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Button(onClick = onAddAccount, modifier = Modifier.padding(top = 12.dp)) {
                        Text("افزودن اولین حساب")
                    }
                }
            }
            return@Column
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = cardColors) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("مجموع موجودی", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    formatMoney(state.total),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (state.total < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                val j = Jalali.today()
                Text(
                    text = "این ماه (" + Jalali.monthNames[j.month - 1] + ")",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
                Text("واریز: " + formatMoney(state.monthIncome), color = MaterialTheme.colorScheme.tertiary)
                Text("خرج: " + formatMoney(state.monthExpense), color = MaterialTheme.colorScheme.error)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onAddTx(0) }) { Text("ثبت خرج") }
            Button(onClick = { onAddTx(1) }) { Text("ثبت واریز") }
        }

        Text("حساب‌ها", style = MaterialTheme.typography.titleMedium)
        state.accounts.forEach { s ->
            Card(
                onClick = { onOpenAccount(s.account.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = cardColors
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.account.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            accountTypeLabels[s.account.type.coerceIn(0, 2)],
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        formatMoney(s.balance),
                        color = if (s.balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        OutlinedButton(onClick = onAddAccount) { Text("افزودن حساب") }

        if (state.recent.isNotEmpty()) {
            Text("آخرین تراکنش‌ها", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            state.recent.forEach { row -> TxRowCard(row, showAccount = true, onClick = { onOpenTx(row.tx.id) }) }
        }
        Column(modifier = Modifier.padding(bottom = 96.dp)) {}
    }
}

@Composable
fun TxRowCard(row: TxRowUi, showAccount: Boolean, onClick: () -> Unit) {
    val tx = row.tx
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.title.ifBlank { tx.category },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val dt = Jalali.dateTimeOf(tx.dateMillis)
                val sub = buildString {
                    append(Jalali.format(Jalali.fromLocalDate(dt.toLocalDate())))
                    append(" · ").append(tx.category)
                    if (showAccount) append(" · ").append(row.accountName)
                }
                Text(
                    sub,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = (if (tx.type == 1) "+" else "-") + formatMoney(tx.amount),
                color = if (tx.type == 1) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
            )
        }
    }
}

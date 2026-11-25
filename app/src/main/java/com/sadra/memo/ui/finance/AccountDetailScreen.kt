package com.sadra.memo.ui.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sadra.memo.MemoApplication
import com.sadra.memo.ui.factoryOf
import com.sadra.memo.util.accountTypeLabels
import com.sadra.memo.util.formatMoney

@Composable
fun AccountDetailScreen(
    accountId: Long,
    onBack: () -> Unit,
    onEditAccount: () -> Unit,
    onOpenTx: (Long) -> Unit
) {
    val app = LocalContext.current.applicationContext as MemoApplication
    val vm: AccountDetailViewModel = viewModel(
        factory = remember(accountId) {
            factoryOf { AccountDetailViewModel(app.container.financeRepository, accountId) }
        }
    )
    val state by vm.state.collectAsState()
    val account = state.account

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
            }
            Text(
                text = account?.name ?: "حساب",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            if (account != null) TextButton(onClick = onEditAccount) { Text("ویرایش") }
        }
        if (state.loaded && account == null) {
            Text("این حساب پیدا نشد.", modifier = Modifier.padding(16.dp))
            return@Column
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (account != null) {
                        Text(
                            accountTypeLabels[account.type.coerceIn(0, 2)],
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        formatMoney(state.balance),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (state.balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "کل واریزها: " + formatMoney(state.income),
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text("کل خرج‌ها: " + formatMoney(state.expense), color = MaterialTheme.colorScheme.error)
                }
            }
            if (state.rows.isEmpty()) {
                Text(
                    "هنوز تراکنشی تو این حساب ثبت نشده.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            state.rows.forEach { row -> TxRowCard(row, showAccount = false, onClick = { onOpenTx(row.tx.id) }) }
        }
    }
}

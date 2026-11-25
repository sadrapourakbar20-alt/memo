package com.sadra.memo.ui.finance

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sadra.memo.MemoApplication
import com.sadra.memo.ui.components.JalaliDatePickerDialog
import com.sadra.memo.ui.factoryOf
import com.sadra.memo.util.Jalali
import com.sadra.memo.util.expenseCategories
import com.sadra.memo.util.formatMoney
import com.sadra.memo.util.incomeCategories

@Composable
fun TxEditScreen(txId: Long, presetType: Int, onClose: () -> Unit, onAddAccount: () -> Unit) {
    val app = LocalContext.current.applicationContext as MemoApplication
    val vm: TxEditViewModel = viewModel(
        factory = remember(txId, presetType) {
            factoryOf { TxEditViewModel(app.container.financeRepository, txId, presetType) }
        }
    )
    val state by vm.state.collectAsState()
    val accounts by vm.accounts.collectAsState()
    var showDelete by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }

    val categories = if (state.type == 0) expenseCategories else incomeCategories
    val amount = state.amountText.toLongOrNull() ?: 0L

    Column(modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت بدون ذخیره")
            }
            Text(
                text = if (txId > 0) "ویرایش تراکنش" else if (state.type == 0) "ثبت خرج" else "ثبت واریز",
                modifier = Modifier.weight(1f)
            )
            if (txId > 0) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.type == 0, onClick = { vm.onType(0) }, label = { Text("خرج") })
                FilterChip(selected = state.type == 1, onClick = { vm.onType(1) }, label = { Text("واریز") })
            }

            OutlinedTextField(
                value = state.amountText,
                onValueChange = vm::onAmount,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                label = { Text("مبلغ (تومان)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { if (amount > 0) Text(formatMoney(amount)) }
            )

            OutlinedTextField(
                value = state.title,
                onValueChange = vm::onTitle,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                label = { Text("توضیح (اختیاری)") },
                singleLine = true
            )

            Text("دسته‌بندی", modifier = Modifier.padding(top = 16.dp))
            LazyRow(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { c ->
                    FilterChip(selected = state.category == c, onClick = { vm.onCategory(c) }, label = { Text(c) })
                }
            }

            Text(
                text = if (state.type == 0) "از کدوم حساب؟" else "به کدوم حساب؟",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp)
            )
            if (accounts.isEmpty()) {
                Text(
                    "هنوز حسابی نداری. اول یه حساب اضافه کن.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Button(onClick = onAddAccount, modifier = Modifier.padding(top = 8.dp)) { Text("افزودن حساب") }
            } else {
                LazyRow(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts, key = { it.id }) { a ->
                        FilterChip(
                            selected = state.accountId == a.id,
                            onClick = { vm.onAccount(a.id) },
                            label = { Text(a.name) }
                        )
                    }
                }
            }

            Text("تاریخ", modifier = Modifier.padding(top = 16.dp))
            OutlinedButton(onClick = { showDate = true }, modifier = Modifier.padding(top = 8.dp)) {
                Text(Jalali.format(Jalali.fromLocalDate(Jalali.dateTimeOf(state.dateMillis).toLocalDate())))
            }

            val error = state.error
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }

    if (showDate) {
        JalaliDatePickerDialog(
            initial = Jalali.fromLocalDate(Jalali.dateTimeOf(state.dateMillis).toLocalDate()),
            onDismiss = { showDate = false },
            onConfirm = { vm.onDate(it); showDate = false }
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("حذف تراکنش") },
            text = { Text("این تراکنش پاک می‌شه و موجودی حساب دوباره حساب می‌شه.") },
            confirmButton = {
                TextButton(onClick = { showDelete = false; vm.deleteAndClose(onClose) }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("انصراف") } }
        )
    }
}

package com.sadra.memo.ui.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import com.sadra.memo.ui.factoryOf
import com.sadra.memo.util.accountTypeLabels
import com.sadra.memo.util.formatMoney

@Composable
fun AccountEditScreen(accountId: Long, onClose: () -> Unit, onDeleted: () -> Unit) {
    val app = LocalContext.current.applicationContext as MemoApplication
    val vm: AccountEditViewModel = viewModel(
        factory = remember(accountId) {
            factoryOf { AccountEditViewModel(app.container.financeRepository, accountId) }
        }
    )
    val state by vm.state.collectAsState()
    var showDelete by remember { mutableStateOf(false) }
    val initial = state.initialText.toLongOrNull() ?: 0L

    Column(modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت بدون ذخیره")
            }
            Text(
                text = if (accountId > 0) "ویرایش حساب" else "حساب جدید",
                modifier = Modifier.weight(1f)
            )
            if (accountId > 0) {
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
                value = state.name,
                onValueChange = vm::onName,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("اسم حساب (مثلاً بانک ملی)") },
                singleLine = true
            )
            Text("نوع حساب", modifier = Modifier.padding(top = 16.dp))
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                accountTypeLabels.forEachIndexed { i, label ->
                    FilterChip(selected = state.type == i, onClick = { vm.onType(i) }, label = { Text(label) })
                }
            }
            OutlinedTextField(
                value = state.initialText,
                onValueChange = vm::onInitial,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                label = { Text("موجودی فعلی (تومان)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { if (initial > 0) Text(formatMoney(initial)) }
            )
            OutlinedTextField(
                value = state.smsKey,
                onValueChange = vm::onSmsKey,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                label = { Text("کلید تشخیص پیامک (اختیاری)") },
                singleLine = true,
                supportingText = { Text("۴ رقم آخر کارت یا حساب، یا اسم فرستنده پیامک بانک. چندتا رو با کاما جدا کن.") }
            )
            Text(
                text = "فقط ۴ رقم آخر کافیه. شماره کامل کارت و رمز نباید اینجا وارد بشه.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            val error = state.error
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("حذف حساب") },
            text = { Text("حساب و همه تراکنش‌هاش برای همیشه پاک می‌شه.") },
            confirmButton = {
                TextButton(onClick = { showDelete = false; vm.deleteAndClose(onDeleted) }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("انصراف") } }
        )
    }
}

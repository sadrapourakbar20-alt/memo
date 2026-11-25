package com.sadra.memo.ui.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadra.memo.data.repository.SmsInboxRepository
import com.sadra.memo.domain.model.AccountSummary
import com.sadra.memo.domain.model.MoneyTx
import com.sadra.memo.domain.model.balanceOf
import com.sadra.memo.domain.repository.FinanceRepository
import com.sadra.memo.util.JDate
import com.sadra.memo.util.Jalali
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TxRowUi(val tx: MoneyTx, val accountName: String)

data class FinanceState(
    val loaded: Boolean = false,
    val accounts: List<AccountSummary> = emptyList(),
    val recent: List<TxRowUi> = emptyList(),
    val total: Long = 0,
    val monthIncome: Long = 0,
    val monthExpense: Long = 0,
    val pendingSms: Int = 0
)

fun currentMonthRange(): Pair<Long, Long> {
    val j = Jalali.today()
    val start = Jalali.toMillis(JDate(j.year, j.month, 1), 0, 0)
    val ny = if (j.month == 12) j.year + 1 else j.year
    val nm = if (j.month == 12) 1 else j.month + 1
    val end = Jalali.toMillis(JDate(ny, nm, 1), 0, 0)
    return start to end
}

class FinanceViewModel(repository: FinanceRepository, inbox: SmsInboxRepository) : ViewModel() {
    val state: StateFlow<FinanceState> = combine(
        repository.observeAccounts(),
        repository.observeTransactions(),
        inbox.observeAll()
    ) { accounts, txs, pending ->
        val (start, end) = currentMonthRange()
        val summaries = accounts.map { AccountSummary(it, balanceOf(it, txs)) }
        val names = accounts.associate { it.id to it.name }
        FinanceState(
            loaded = true,
            accounts = summaries,
            recent = txs.take(15).map { TxRowUi(it, names[it.accountId] ?: "؟") },
            total = summaries.sumOf { it.balance },
            monthIncome = txs.filter { it.type == 1 && it.dateMillis in start until end }.sumOf { it.amount },
            monthExpense = txs.filter { it.type == 0 && it.dateMillis in start until end }.sumOf { it.amount },
            pendingSms = pending.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinanceState())
}

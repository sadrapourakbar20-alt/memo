package com.sadra.memo.ui.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadra.memo.domain.model.Account
import com.sadra.memo.domain.model.balanceOf
import com.sadra.memo.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AccountDetailState(
    val loaded: Boolean = false,
    val account: Account? = null,
    val balance: Long = 0,
    val income: Long = 0,
    val expense: Long = 0,
    val rows: List<TxRowUi> = emptyList()
)

class AccountDetailViewModel(repository: FinanceRepository, accountId: Long) : ViewModel() {
    val state: StateFlow<AccountDetailState> = combine(
        repository.observeAccounts(),
        repository.observeTransactions()
    ) { accounts, txs ->
        val account = accounts.firstOrNull { it.id == accountId }
        val mine = txs.filter { it.accountId == accountId }
        AccountDetailState(
            loaded = true,
            account = account,
            balance = if (account != null) balanceOf(account, txs) else 0,
            income = mine.filter { it.type == 1 }.sumOf { it.amount },
            expense = mine.filter { it.type == 0 }.sumOf { it.amount },
            rows = mine.map { TxRowUi(it, account?.name ?: "؟") }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountDetailState())
}

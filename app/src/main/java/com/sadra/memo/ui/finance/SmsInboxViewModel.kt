package com.sadra.memo.ui.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadra.memo.data.repository.SmsInboxRepository
import com.sadra.memo.domain.model.Account
import com.sadra.memo.domain.model.MoneyTx
import com.sadra.memo.domain.model.SmsInboxItem
import com.sadra.memo.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SmsInboxViewModel(
    private val inbox: SmsInboxRepository,
    private val finance: FinanceRepository
) : ViewModel() {

    val items: StateFlow<List<SmsInboxItem>> = inbox.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<Account>> = finance.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun assign(item: SmsInboxItem, accountId: Long) {
        viewModelScope.launch {
            finance.saveTransaction(
                MoneyTx(
                    accountId = accountId,
                    type = item.type,
                    amount = item.amount,
                    title = "پیامک بانکی",
                    category = item.category,
                    dateMillis = item.dateMillis,
                    createdAt = System.currentTimeMillis(),
                    smsHash = item.hash
                )
            )
            inbox.delete(item.id)
        }
    }

    fun ignore(item: SmsInboxItem) {
        viewModelScope.launch { inbox.delete(item.id) }
    }
}

package com.sadra.memo.ui.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadra.memo.data.repository.SmsInboxRepository
import com.sadra.memo.domain.model.Account
import com.sadra.memo.domain.model.MoneyTx
import com.sadra.memo.domain.model.SmsInboxItem
import com.sadra.memo.domain.repository.FinanceRepository
import com.sadra.memo.sms.SmsProcessor
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

    fun assign(item: SmsInboxItem, accountId: Long, rememberSender: Boolean = false) {
        viewModelScope.launch {
            val account = finance.getAccount(accountId)
            if (rememberSender && account != null && item.sender.isNotBlank()) {
                val cleanSender = item.sender.trim()
                val alreadyHas = SmsProcessor.matchesSender(account.smsKey, cleanSender)
                if (!alreadyHas) {
                    val updatedKey = if (account.smsKey.isBlank()) cleanSender else "${account.smsKey}, $cleanSender"
                    finance.saveAccount(account.copy(smsKey = updatedKey))
                }
            }
            val title = if (account != null) "پیامک ${account.name}" else "پیامک بانکی"
            finance.saveTransaction(
                MoneyTx(
                    accountId = accountId,
                    type = item.type,
                    amount = item.amount,
                    title = title,
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

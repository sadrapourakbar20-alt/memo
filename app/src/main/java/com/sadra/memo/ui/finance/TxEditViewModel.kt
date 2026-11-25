package com.sadra.memo.ui.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadra.memo.domain.model.Account
import com.sadra.memo.domain.model.MoneyTx
import com.sadra.memo.domain.nlu.TextNorm
import com.sadra.memo.domain.repository.FinanceRepository
import com.sadra.memo.util.JDate
import com.sadra.memo.util.Jalali
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TxEditState(
    val type: Int = 0,
    val amountText: String = "",
    val title: String = "",
    val category: String = "سایر",
    val accountId: Long? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val error: String? = null,
    val loaded: Boolean = false
)

class TxEditViewModel(
    private val repository: FinanceRepository,
    private val txId: Long,
    presetType: Int
) : ViewModel() {

    private val _state = MutableStateFlow(TxEditState(type = presetType, loaded = txId <= 0))
    val state: StateFlow<TxEditState> = _state

    val accounts: StateFlow<List<Account>> = repository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var original: MoneyTx? = null

    init {
        if (txId > 0) {
            viewModelScope.launch {
                val tx = repository.getTransaction(txId)
                original = tx
                _state.value = if (tx != null) {
                    TxEditState(
                        type = tx.type,
                        amountText = tx.amount.toString(),
                        title = tx.title,
                        category = tx.category,
                        accountId = tx.accountId,
                        dateMillis = tx.dateMillis,
                        loaded = true
                    )
                } else TxEditState(type = presetType, loaded = true)
            }
        }
    }

    fun onType(v: Int) { _state.value = _state.value.copy(type = v, category = "سایر", error = null) }

    fun onAmount(v: String) {
        val digits = TextNorm.digits(v).filter { it.isDigit() }.take(13)
        _state.value = _state.value.copy(amountText = digits, error = null)
    }

    fun onTitle(v: String) { _state.value = _state.value.copy(title = v) }
    fun onCategory(v: String) { _state.value = _state.value.copy(category = v) }
    fun onAccount(id: Long) { _state.value = _state.value.copy(accountId = id, error = null) }

    fun onDate(j: JDate) {
        val old = Jalali.dateTimeOf(_state.value.dateMillis)
        _state.value = _state.value.copy(dateMillis = Jalali.toMillis(j, old.hour, old.minute))
    }

    fun saveAndClose(onDone: () -> Unit) {
        val s = _state.value
        val amount = s.amountText.toLongOrNull() ?: 0L
        if (amount <= 0L) {
            _state.value = s.copy(error = "مبلغ رو وارد کن.")
            return
        }
        val accountId = s.accountId
        if (accountId == null) {
            _state.value = s.copy(error = if (s.type == 0) "از کدوم حساب؟ یکی رو انتخاب کن." else "به کدوم حساب؟ یکی رو انتخاب کن.")
            return
        }
        viewModelScope.launch {
            repository.saveTransaction(
                MoneyTx(
                    id = original?.id ?: 0,
                    accountId = accountId,
                    type = s.type,
                    amount = amount,
                    title = s.title.trim(),
                    category = s.category,
                    dateMillis = s.dateMillis,
                    createdAt = original?.createdAt ?: System.currentTimeMillis()
                )
            )
            onDone()
        }
    }

    fun deleteAndClose(onDone: () -> Unit) {
        viewModelScope.launch {
            if (txId > 0) repository.deleteTransaction(txId)
            onDone()
        }
    }
}

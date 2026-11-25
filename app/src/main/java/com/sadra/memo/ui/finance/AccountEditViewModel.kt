package com.sadra.memo.ui.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadra.memo.domain.model.Account
import com.sadra.memo.domain.nlu.TextNorm
import com.sadra.memo.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AccountEditState(
    val name: String = "",
    val type: Int = 0,
    val initialText: String = "",
    val error: String? = null,
    val loaded: Boolean = false
)

class AccountEditViewModel(
    private val repository: FinanceRepository,
    private val accountId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(AccountEditState(loaded = accountId <= 0))
    val state: StateFlow<AccountEditState> = _state

    private var original: Account? = null

    init {
        if (accountId > 0) {
            viewModelScope.launch {
                val a = repository.getAccount(accountId)
                original = a
                _state.value = if (a != null) {
                    AccountEditState(a.name, a.type, a.initialBalance.toString(), loaded = true)
                } else AccountEditState(loaded = true)
            }
        }
    }

    fun onName(v: String) { _state.value = _state.value.copy(name = v, error = null) }
    fun onType(v: Int) { _state.value = _state.value.copy(type = v) }
    fun onInitial(v: String) {
        _state.value = _state.value.copy(initialText = TextNorm.digits(v).filter { it.isDigit() }.take(13))
    }

    fun saveAndClose(onDone: () -> Unit) {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.value = s.copy(error = "اسم حساب رو وارد کن.")
            return
        }
        viewModelScope.launch {
            repository.saveAccount(
                Account(
                    id = original?.id ?: 0,
                    name = s.name.trim(),
                    type = s.type,
                    initialBalance = s.initialText.toLongOrNull() ?: 0L,
                    createdAt = original?.createdAt ?: System.currentTimeMillis()
                )
            )
            onDone()
        }
    }

    fun deleteAndClose(onDone: () -> Unit) {
        viewModelScope.launch {
            if (accountId > 0) repository.deleteAccount(accountId)
            onDone()
        }
    }
}

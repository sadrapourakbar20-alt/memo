package com.sadra.memo.domain.repository

import com.sadra.memo.domain.model.Account
import com.sadra.memo.domain.model.MoneyTx
import kotlinx.coroutines.flow.Flow

interface FinanceRepository {
    fun observeAccounts(): Flow<List<Account>>
    fun observeTransactions(): Flow<List<MoneyTx>>
    suspend fun getAccounts(): List<Account>
    suspend fun getAllTransactions(): List<MoneyTx>
    suspend fun getAccount(id: Long): Account?
    suspend fun saveAccount(account: Account): Long
    suspend fun deleteAccount(id: Long)
    suspend fun getTransaction(id: Long): MoneyTx?
    suspend fun saveTransaction(tx: MoneyTx): Long
    suspend fun deleteTransaction(id: Long)
    suspend fun hasSmsHash(hash: String): Boolean
}

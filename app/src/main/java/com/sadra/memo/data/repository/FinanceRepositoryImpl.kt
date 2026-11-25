package com.sadra.memo.data.repository

import com.sadra.memo.data.dao.FinanceDao
import com.sadra.memo.data.entities.toDomain
import com.sadra.memo.data.entities.toEntity
import com.sadra.memo.domain.model.Account
import com.sadra.memo.domain.model.MoneyTx
import com.sadra.memo.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FinanceRepositoryImpl(private val dao: FinanceDao) : FinanceRepository {
    override fun observeAccounts(): Flow<List<Account>> =
        dao.observeAccounts().map { l -> l.map { it.toDomain() } }

    override fun observeTransactions(): Flow<List<MoneyTx>> =
        dao.observeTransactions().map { l -> l.map { it.toDomain() } }

    override suspend fun getAccounts(): List<Account> = dao.getAccounts().map { it.toDomain() }

    override suspend fun getAllTransactions(): List<MoneyTx> =
        dao.getAllTransactions().map { it.toDomain() }

    override suspend fun getAccount(id: Long): Account? = dao.getAccount(id)?.toDomain()

    override suspend fun saveAccount(account: Account): Long = dao.upsertAccount(account.toEntity())

    override suspend fun deleteAccount(id: Long) {
        dao.deleteTransactionsOfAccount(id)
        dao.deleteAccount(id)
    }

    override suspend fun getTransaction(id: Long): MoneyTx? = dao.getTransaction(id)?.toDomain()

    override suspend fun saveTransaction(tx: MoneyTx): Long = dao.upsertTransaction(tx.toEntity())

    override suspend fun deleteTransaction(id: Long) = dao.deleteTransaction(id)

    override suspend fun hasSmsHash(hash: String): Boolean = dao.countByHash(hash) > 0
}

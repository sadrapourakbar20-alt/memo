package com.sadra.memo.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sadra.memo.domain.model.Account
import com.sadra.memo.domain.model.Attachment
import com.sadra.memo.domain.model.MoneyTx

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: Int,
    val initialBalance: Long,
    val createdAt: Long
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val type: Int,
    val amount: Long,
    val title: String,
    val category: String,
    val dateMillis: Long,
    val createdAt: Long
)

@Entity(tableName = "attachments")
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val kind: Int,
    val name: String,
    val path: String,
    val mime: String,
    val createdAt: Long
)

fun AccountEntity.toDomain() = Account(id, name, type, initialBalance, createdAt)
fun Account.toEntity() = AccountEntity(id, name, type, initialBalance, createdAt)

fun TransactionEntity.toDomain() =
    MoneyTx(id, accountId, type, amount, title, category, dateMillis, createdAt)

fun MoneyTx.toEntity() =
    TransactionEntity(id, accountId, type, amount, title, category, dateMillis, createdAt)

fun AttachmentEntity.toDomain() = Attachment(id, noteId, kind, name, path, mime, createdAt)
fun Attachment.toEntity() = AttachmentEntity(id, noteId, kind, name, path, mime, createdAt)

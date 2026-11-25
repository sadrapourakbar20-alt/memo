package com.sadra.memo.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sadra.memo.domain.model.Account
import com.sadra.memo.domain.model.Attachment
import com.sadra.memo.domain.model.MoneyTx
import com.sadra.memo.domain.model.SmsInboxItem

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: Int,
    val initialBalance: Long,
    val createdAt: Long,
    val smsKey: String = ""
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
    val createdAt: Long,
    val smsHash: String = ""
)

@Entity(tableName = "sms_inbox")
data class SmsInboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hash: String,
    val sender: String,
    val body: String,
    val type: Int,
    val amount: Long,
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

fun AccountEntity.toDomain() = Account(id, name, type, initialBalance, createdAt, smsKey)
fun Account.toEntity() = AccountEntity(id, name, type, initialBalance, createdAt, smsKey)

fun TransactionEntity.toDomain() =
    MoneyTx(id, accountId, type, amount, title, category, dateMillis, createdAt, smsHash)

fun MoneyTx.toEntity() =
    TransactionEntity(id, accountId, type, amount, title, category, dateMillis, createdAt, smsHash)

fun SmsInboxEntity.toDomain() = SmsInboxItem(id, hash, sender, body, type, amount, category, dateMillis, createdAt)
fun SmsInboxItem.toEntity() = SmsInboxEntity(id, hash, sender, body, type, amount, category, dateMillis, createdAt)

fun AttachmentEntity.toDomain() = Attachment(id, noteId, kind, name, path, mime, createdAt)
fun Attachment.toEntity() = AttachmentEntity(id, noteId, kind, name, path, mime, createdAt)

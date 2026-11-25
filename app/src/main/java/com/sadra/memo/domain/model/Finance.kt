package com.sadra.memo.domain.model

data class Account(
    val id: Long = 0,
    val name: String,
    val type: Int = 0,
    val initialBalance: Long = 0,
    val createdAt: Long,
    /** کلید تشخیص پیامک: ۴ رقم آخر کارت/حساب یا اسم فرستنده؛ چندتا رو با کاما جدا کن */
    val smsKey: String = ""
)

/** type: ۰ خرج، ۱ واریز */
data class MoneyTx(
    val id: Long = 0,
    val accountId: Long,
    val type: Int,
    val amount: Long,
    val title: String = "",
    val category: String = "سایر",
    val dateMillis: Long,
    val createdAt: Long,
    val smsHash: String = ""
)

data class SmsInboxItem(
    val id: Long = 0,
    val hash: String,
    val sender: String,
    val body: String,
    val type: Int,
    val amount: Long,
    val category: String,
    val dateMillis: Long,
    val createdAt: Long
)

/** kind: ۰ فایل، ۱ تصویر، ۲ صدا */
data class Attachment(
    val id: Long = 0,
    val noteId: Long,
    val kind: Int,
    val name: String,
    val path: String,
    val mime: String,
    val createdAt: Long
)

data class AccountSummary(val account: Account, val balance: Long)

fun balanceOf(account: Account, txs: List<MoneyTx>): Long {
    var b = account.initialBalance
    for (t in txs) {
        if (t.accountId != account.id) continue
        b += if (t.type == 1) t.amount else -t.amount
    }
    return b
}

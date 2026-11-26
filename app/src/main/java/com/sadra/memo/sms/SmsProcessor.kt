package com.sadra.memo.sms

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sadra.memo.MainActivity
import com.sadra.memo.R
import com.sadra.memo.data.preferences.SettingsRepository
import com.sadra.memo.data.repository.SmsInboxRepository
import com.sadra.memo.domain.model.Account
import com.sadra.memo.domain.model.MoneyTx
import com.sadra.memo.domain.model.SmsInboxItem
import com.sadra.memo.domain.nlu.SmsParser
import com.sadra.memo.domain.nlu.TextNorm
import com.sadra.memo.domain.repository.FinanceRepository
import com.sadra.memo.reminder.NotificationHelper
import com.sadra.memo.util.formatMoney
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest

class SmsProcessor(
    private val context: Context,
    private val finance: FinanceRepository,
    private val inbox: SmsInboxRepository,
    private val settings: SettingsRepository
) {
    private val mutex = Mutex()

    companion object {
        fun normalizeSender(raw: String): String {
            val trimmed = TextNorm.digits(raw).trim()
            if (trimmed.isEmpty()) return ""
            val digitsOnly = trimmed.filter { it.isDigit() }
            val isPhoneLike = digitsOnly.length >= 7 && trimmed.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }
            if (isPhoneLike) {
                return when {
                    digitsOnly.startsWith("0098") && digitsOnly.length > 4 -> "0" + digitsOnly.drop(4)
                    digitsOnly.startsWith("98") && digitsOnly.length >= 11 -> "0" + digitsOnly.drop(2)
                    else -> digitsOnly
                }
            }
            return trimmed
                .lowercase()
                .replace(".", "")
                .replace("-", "")
                .replace("_", "")
                .replace("\s+".toRegex(), " ")
                .trim()
        }

        private fun splitKeys(keys: String): List<String> =
            keys.split(',', '،', '\n', ';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }

        fun matchesBody(keys: String, body: String): Boolean {
            val normBody = TextNorm.digits(body).lowercase()
            return splitKeys(keys).any { rawKey ->
                val k = TextNorm.digits(rawKey).lowercase()
                k.isNotEmpty() && normBody.contains(k)
            }
        }

        fun matchesSender(keys: String, sender: String): Boolean {
            val normSender = normalizeSender(sender)
            if (normSender.isEmpty()) return false
            return splitKeys(keys).any { rawKey ->
                val normKey = normalizeSender(rawKey)
                normKey.isNotEmpty() && (
                    normSender.equals(normKey, ignoreCase = true) ||
                        (normKey.length >= 4 && normSender.endsWith(normKey))
                )
            }
        }
    }

    private fun hashOf(sender: String, body: String, ts: Long): String {
        val raw = normalizeSender(sender) + "|" + body.trim() + "|" + (ts / 60_000L)
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(24)
    }

    private fun findMatchingAccounts(accounts: List<Account>, sender: String, body: String): List<Account> {
        val keyed = accounts.filter { it.smsKey.isNotBlank() }
        // اولویت اول: تطبیق با متن پیامک (مثل بلو که اسم بانک در متن میاد)
        val bodyMatches = keyed.filter { matchesBody(it.smsKey, body) }
        if (bodyMatches.isNotEmpty()) return bodyMatches

        // اولویت دوم: وقتی در متن اسمی نبود، بررسی سرشماره یا نام فرستنده (مثل B.Pasargad یا 09999987641)
        return keyed.filter { matchesSender(it.smsKey, sender) }
    }

    suspend fun process(sender: String, body: String, timestamp: Long, showNotification: Boolean = true) {
        mutex.withLock {
            if (!settings.smsEnabled.first()) return
            val accounts = finance.getAccounts()
            val candidates = findMatchingAccounts(accounts, sender, body)
            val fallbackName = if (candidates.size == 1) candidates[0].name else null

            val parsed = SmsParser.parse(body, settings.smsRial.first(), fallbackName) ?: return
            val hash = hashOf(sender, body, timestamp)
            if (inbox.exists(hash) || finance.hasSmsHash(hash)) return

            val kind = if (parsed.type == 0) "برداشت" else "واریز"
            val auto = settings.smsAuto.first()

            if (candidates.size == 1 && auto) {
                val account = candidates[0]
                finance.saveTransaction(
                    MoneyTx(
                        accountId = account.id,
                        type = parsed.type,
                        amount = parsed.amount,
                        title = parsed.title,
                        category = parsed.category,
                        dateMillis = timestamp,
                        createdAt = System.currentTimeMillis(),
                        smsHash = hash
                    )
                )
                if (showNotification) {
                    notify(
                        hash,
                        "ثبت شد: $kind ${formatMoney(parsed.amount)}",
                        "تو حساب «${account.name}» ثبت شد."
                    )
                }
            } else {
                inbox.add(
                    SmsInboxItem(
                        hash = hash,
                        sender = sender,
                        body = body.take(200),
                        type = parsed.type,
                        amount = parsed.amount,
                        category = parsed.category,
                        dateMillis = timestamp,
                        createdAt = System.currentTimeMillis()
                    )
                )
                if (showNotification) {
                    notify(
                        hash,
                        "$kind ${formatMoney(parsed.amount)} منتظر تأییده",
                        "باز کن و حساب رو انتخاب کن."
                    )
                }
            }
        }
    }

    /** همگام‌سازی پیامک‌های اخیر صندوق پیام گوشی (برای مواقعی که سیستم‌عامل دریافت در پس‌زمینه رو محدود کرده) */
    suspend fun syncRecentFromDeviceInbox(lookbackHours: Long = 48L) {
        if (!settings.smsEnabled.first()) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val since = System.currentTimeMillis() - lookbackHours * 60L * 60L * 1000L
        val messages = mutableListOf<Triple<String, String, Long>>()
        try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                "${Telephony.Sms.DATE} >= ?",
                arrayOf(since.toString()),
                "${Telephony.Sms.DATE} ASC"
            )?.use { cursor ->
                val idxAddr = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val idxBody = cursor.getColumnIndex(Telephony.Sms.BODY)
                val idxDate = cursor.getColumnIndex(Telephony.Sms.DATE)
                while (cursor.moveToNext()) {
                    val addr = if (idxAddr >= 0) cursor.getString(idxAddr).orEmpty() else ""
                    val body = if (idxBody >= 0) cursor.getString(idxBody).orEmpty() else ""
                    val date = if (idxDate >= 0) cursor.getLong(idxDate) else System.currentTimeMillis()
                    if (body.isNotBlank()) {
                        messages.add(Triple(addr, body, date))
                    }
                }
            }
        } catch (_: Exception) {
            return
        }
        for ((sender, body, ts) in messages) {
            process(sender, body, ts, showNotification = false)
        }
    }

    private fun notify(hash: String, title: String, text: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val open = PendingIntent.getActivity(
            context,
            hash.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, NotificationHelper.channelId(context))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            manager.notify(hash.hashCode(), n)
        } catch (_: SecurityException) {
        }
    }
}

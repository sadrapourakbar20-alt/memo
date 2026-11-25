package com.sadra.memo.sms

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.sadra.memo.MainActivity
import com.sadra.memo.R
import com.sadra.memo.data.preferences.SettingsRepository
import com.sadra.memo.data.repository.SmsInboxRepository
import com.sadra.memo.domain.model.MoneyTx
import com.sadra.memo.domain.model.SmsInboxItem
import com.sadra.memo.domain.nlu.SmsParser
import com.sadra.memo.domain.nlu.TextNorm
import com.sadra.memo.domain.repository.FinanceRepository
import com.sadra.memo.reminder.NotificationHelper
import com.sadra.memo.util.formatMoney
import kotlinx.coroutines.flow.first
import java.security.MessageDigest

class SmsProcessor(
    private val context: Context,
    private val finance: FinanceRepository,
    private val inbox: SmsInboxRepository,
    private val settings: SettingsRepository
) {
    private fun hashOf(sender: String, body: String, ts: Long): String {
        val raw = sender + "|" + body + "|" + (ts / 60_000L)
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(24)
    }

    private fun matches(keys: String, sender: String, body: String): Boolean {
        val normBody = TextNorm.digits(body)
        return keys.split(',', '،', ' ', '\n')
            .map { TextNorm.digits(it.trim()) }
            .filter { it.isNotEmpty() }
            .any { k -> sender.equals(k, ignoreCase = true) || normBody.contains(k, ignoreCase = true) }
    }

    /** نتیجه: همون چیزی که برای تست تو تنظیمات نشون می‌دیم یا null اگه پیامک تراکنش نبود */
    suspend fun process(sender: String, body: String, timestamp: Long) {
        if (!settings.smsEnabled.first()) return
        val parsed = SmsParser.parse(body, settings.smsRial.first()) ?: return
        val hash = hashOf(sender, body, timestamp)
        if (inbox.exists(hash) || finance.hasSmsHash(hash)) return

        val accounts = finance.getAccounts()
        val candidates = accounts.filter { it.smsKey.isNotBlank() && matches(it.smsKey, sender, body) }
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
            notify(
                hash,
                "ثبت شد: $kind ${formatMoney(parsed.amount)}",
                "تو حساب «${account.name}» ثبت شد."
            )
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
            notify(
                hash,
                "$kind ${formatMoney(parsed.amount)} منتظر تأییده",
                "باز کن و حساب رو انتخاب کن."
            )
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
        } catch (e: SecurityException) {
            // مجوز اعلان داده نشده
        }
    }
}

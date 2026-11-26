package com.sadra.memo.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Telephony
import com.sadra.memo.MemoApplication
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val grouped = linkedMapOf<String, MutableList<android.telephony.SmsMessage>>()
        for (msg in messages) {
            val sender = (msg.displayOriginatingAddress ?: msg.originatingAddress ?: "").trim()
            grouped.getOrPut(sender) { mutableListOf() }.add(msg)
        }

        val app = context.applicationContext as MemoApplication
        val pending = goAsync()
        val pm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Memo:SmsReceiver")?.apply {
            setReferenceCounted(false)
            acquire(15_000L)
        }

        app.container.appScope.launch {
            try {
                for ((sender, parts) in grouped) {
                    val body = parts.joinToString("") { it.messageBody ?: "" }
                    val ts = parts.firstOrNull()?.timestampMillis?.takeIf { it > 0 } ?: System.currentTimeMillis()
                    app.container.smsProcessor.process(sender, body, ts)
                }
            } finally {
                try {
                    if (wakeLock?.isHeld == true) wakeLock.release()
                } catch (_: Exception) {
                }
                pending.finish()
            }
        }
    }
}

package com.sadra.memo.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.sadra.memo.MemoApplication
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return
        val sender = messages[0].originatingAddress ?: ""
        val body = messages.joinToString("") { it.messageBody ?: "" }
        val ts = messages[0].timestampMillis.takeIf { it > 0 } ?: System.currentTimeMillis()
        val app = context.applicationContext as MemoApplication
        val pending = goAsync()
        app.container.appScope.launch {
            try {
                app.container.smsProcessor.process(sender, body, ts)
            } finally {
                pending.finish()
            }
        }
    }
}

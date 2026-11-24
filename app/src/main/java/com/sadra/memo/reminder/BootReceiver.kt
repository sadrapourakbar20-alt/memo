package com.sadra.memo.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sadra.memo.MemoApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as MemoApplication
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.container.taskManager.rescheduleAll()
                app.container.classScheduler.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}

package com.sadra.memo.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri

/**
 * صدای اعلان روی خود کانال ذخیره می‌شه و بعد از ساخت قابل تغییر نیست؛
 * پس با هر تغییر صدا یه کانال جدید می‌سازیم.
 */
object NotificationHelper {
    private const val PREFS = "memo_notifications"
    private const val KEY_CHANNEL = "channel_id"
    private const val KEY_SOUND = "sound_uri"
    const val DEFAULT_ID = "reminders"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun channelId(context: Context): String = prefs(context).getString(KEY_CHANNEL, DEFAULT_ID) ?: DEFAULT_ID

    fun soundUri(context: Context): String? = prefs(context).getString(KEY_SOUND, null)

    private fun create(context: Context, id: String, sound: Uri?) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(id, "یادآوری‌ها", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "یادآوری کارها و کلاس‌ها"
            enableVibration(true)
            if (sound != null) {
                setSound(
                    sound,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
        }
        manager.createNotificationChannel(channel)
    }

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val id = channelId(context)
        if (manager.getNotificationChannel(id) == null) {
            create(context, id, soundUri(context)?.let { Uri.parse(it) })
        }
    }

    fun setSound(context: Context, sound: Uri?) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val oldId = channelId(context)
        val newId = "reminders_" + System.currentTimeMillis()
        create(context, newId, sound)
        prefs(context).edit()
            .putString(KEY_CHANNEL, newId)
            .putString(KEY_SOUND, sound?.toString())
            .apply()
        manager.deleteNotificationChannel(oldId)
    }
}

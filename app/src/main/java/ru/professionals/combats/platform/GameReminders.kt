package ru.professionals.combats.platform

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import ru.professionals.combats.MainActivity
import ru.professionals.combats.R

/** Persists reminders and schedules them one hour before a combat. Created 30-09-2026; author: training project. */
class GameReminders(private val context: Context) {
    private val preferences = context.getSharedPreferences("game_reminders", Context.MODE_PRIVATE)
    fun schedule(id: String, title: String, startsAtEpochMillis: Long) {
        val at = startsAtEpochMillis - 60 * 60 * 1000
        if (at <= System.currentTimeMillis()) return
        preferences.edit().putString(id, JSONObject().put("title", title).put("at", at).toString()).apply()
        val intent = Intent(context, GameReminderReceiver::class.java).setData(Uri.parse("combats://reminder/$id"))
            .putExtra("title", title).putExtra("id", id)
        val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // Standard alarms require no special exact-alarm permission; Android may delay during Doze.
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        Log.i("GameReminders", "[GameReminders]: Schedule — Reminder registered")
    }
    fun cancel(id: String) {
        val intent = Intent(context, GameReminderReceiver::class.java).setData(Uri.parse("combats://reminder/$id"))
        val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        pending?.let { context.getSystemService(AlarmManager::class.java).cancel(it); it.cancel() }
        preferences.edit().remove(id).apply()
    }
    fun restore() {
        preferences.all.forEach { (id, value) ->
            try {
                val data = JSONObject(value as String)
                if (data.getLong("at") > System.currentTimeMillis()) schedule(id, data.getString("title"), data.getLong("at") + 3_600_000)
                else preferences.edit().remove(id).apply()
            } catch (e: Exception) { Log.e("GameReminders", "[GameReminders]: Restore — Invalid saved reminder", e) }
        }
    }
}

/** Delivers a local notification; permission is requested from the scheduling screen. */
class GameReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("combats", "Game reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, "combats").setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(intent.getStringExtra("title") ?: "Puzzle Combats").setContentText("Your combat starts in one hour")
            .setContentIntent(open).setAutoCancel(true).build()
        NotificationManagerCompat.from(context).notify((intent.getStringExtra("id") ?: "combat").hashCode(), notification)
    }
}

/** Re-registers persisted reminders after the device restarts. */
class ReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { if (intent.action == Intent.ACTION_BOOT_COMPLETED) GameReminders(context).restore() }
}

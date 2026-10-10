package app.unhook.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import app.unhook.MainActivity
import app.unhook.R
import app.unhook.container
import app.unhook.domain.Milestone
import app.unhook.ui.Stock
import app.unhook.widget.CounterWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** One alarm at the soonest upcoming trophy across all habits; celebrates every rung crossed since the last check. */
object Milestones {
    private const val CHANNEL_ID = "milestones_v1"
    const val ACTION_FIRE = "app.unhook.notify.FIRE"
    const val EXTRA_HABIT_ID = "habit_id"
    private const val PREFS = "milestones"
    private const val LAST_CHECK = "last_check"

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(R.string.channel_desc)
        }
        nm.createNotificationChannel(channel)
    }

    private fun fireIntent(context: Context) = PendingIntent.getBroadcast(
        context, 1,
        Intent(context, MilestoneReceiver::class.java).setAction(ACTION_FIRE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    suspend fun reschedule(context: Context) {
        val now = System.currentTimeMillis()
        val prefs = prefs(context)
        if (!prefs.contains(LAST_CHECK)) prefs.edit { putLong(LAST_CHECK, now) }
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = fireIntent(context)
        val habits = context.container.db.habits().all()
        val next = habits.mapNotNull { h -> Milestone.next(now - h.streakStart)?.let { h.streakStart + it.ms } }.minOrNull()
        // The widget counts days, so wake every few hours to keep it current even without a trophy due.
        val heartbeat = now + 6 * 60 * 60 * 1000L
        val at = minOf(next ?: heartbeat, heartbeat)
        // A trophy does not need to-the-second precision; this avoids the SCHEDULE_EXACT_ALARM permission.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    suspend fun fire(context: Context) {
        val now = System.currentTimeMillis()
        val prefs = prefs(context)
        val since = prefs.getLong(LAST_CHECK, now)
        prefs.edit { putLong(LAST_CHECK, now) }
        if (!canNotify(context) || !context.container.settings.now().notifications) return
        val habits = context.container.db.habits().all()
        for (h in habits) {
            // Only the highest rung crossed in the window: no burst of notifications after a long sleep.
            val crossed = Milestone.entries.lastOrNull { h.streakStart + it.ms in (since + 1)..now } ?: continue
            notify(context, h.id, Stock.name(context, h.name), crossed)
        }
    }

    private fun notify(context: Context, habitId: Long, name: String, m: Milestone) {
        val open = PendingIntent.getActivity(
            context, habitId.toInt(),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_HABIT_ID, habitId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val period = milestoneText(context, m)
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.accent))
            .setContentTitle(context.getString(R.string.notif_title, period))
            .setContentText(context.getString(R.string.notif_text, name))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(1000 + habitId.toInt(), n)
        } catch (_: SecurityException) {
        }
    }

    fun milestoneText(context: Context, m: Milestone): String = context.getString(
        when (m) {
            Milestone.DAY_1 -> R.string.m_day_1
            Milestone.DAY_3 -> R.string.m_day_3
            Milestone.WEEK_1 -> R.string.m_week_1
            Milestone.WEEK_2 -> R.string.m_week_2
            Milestone.MONTH_1 -> R.string.m_month_1
            Milestone.MONTH_3 -> R.string.m_month_3
            Milestone.MONTH_6 -> R.string.m_month_6
            Milestone.YEAR_1 -> R.string.m_year_1
            Milestone.YEAR_2 -> R.string.m_year_2
            Milestone.YEAR_5 -> R.string.m_year_5
        }
    )
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class MilestoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Milestones.ACTION_FIRE) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Milestones.fire(context)
                Milestones.reschedule(context)
                CounterWidget.refresh(context)
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                context.container.changed()
            } finally {
                pending.finish()
            }
        }
    }
}

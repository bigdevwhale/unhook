package app.unhook.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import app.unhook.MainActivity
import app.unhook.R
import app.unhook.container
import app.unhook.domain.DAY_MS
import app.unhook.notify.Milestones
import app.unhook.ui.Stock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Home-screen counter: clean days for up to three habits; a tap opens the habit. */
class CounterWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                refresh(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val rows = listOf(
            Triple(R.id.row1, R.id.days1, R.id.name1),
            Triple(R.id.row2, R.id.days2, R.id.name2),
            Triple(R.id.row3, R.id.days3, R.id.name3),
        )

        suspend fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CounterWidget::class.java))
            if (ids.isEmpty()) return
            val habits = context.container.db.habits().all().take(rows.size)
            val now = System.currentTimeMillis()
            val views = RemoteViews(context.packageName, R.layout.widget_counter)
            views.setViewVisibility(R.id.empty, if (habits.isEmpty()) View.VISIBLE else View.GONE)
            views.setOnClickPendingIntent(R.id.root, open(context, 0))
            rows.forEachIndexed { i, (row, days, name) ->
                val h = habits.getOrNull(i)
                views.setViewVisibility(row, if (h == null) View.GONE else View.VISIBLE)
                if (h != null) {
                    val d = ((now - h.streakStart) / DAY_MS).toInt()
                    views.setTextViewText(days, d.toString())
                    views.setTextViewText(name, context.resources.getQuantityString(R.plurals.widget_days, d, Stock.name(context, h.name)))
                    views.setOnClickPendingIntent(row, open(context, h.id))
                }
            }
            manager.updateAppWidget(ids, views)
        }

        private fun open(context: Context, habitId: Long): PendingIntent = PendingIntent.getActivity(
            context, 5000 + habitId.toInt(),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(Milestones.EXTRA_HABIT_ID, habitId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

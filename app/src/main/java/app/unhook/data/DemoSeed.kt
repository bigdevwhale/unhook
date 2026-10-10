package app.unhook.data

import android.content.Context
import app.unhook.domain.DAY_MS
import java.util.Locale

/** Made-up habits for README screenshots; only compiled into effect with `-Pdemo` (app.unhook.demo). */
object DemoSeed {
    suspend fun seedIfEmpty(context: Context, db: UnhookDatabase, settings: SettingsStore) {
        val dao = db.habits()
        if (dao.all().isNotEmpty()) return
        val ru = Locale.getDefault().language == "ru"
        settings.setCurrency(if (ru) "RUB" else "USD")
        val now = System.currentTimeMillis()
        val hour = 3_600_000L
        val res = context.resources

        val smokingQuit = now - 61 * DAY_MS - 5 * hour
        val smoking = Habit(
            name = "Smoking", kind = "smoking", quitAt = smokingQuit, streakStart = now - 38 * DAY_MS - 5 * hour,
            times = 15.0, money = if (ru) 300.0 else 9.0, minutes = 70.0, createdAt = smokingQuit,
        )
        val sid = dao.insert(smoking)
        val s = smoking.copy(id = sid)
        // Two earlier streaks: 9 days and 14 days.
        dao.insertRelapses(
            listOf(
                Relapse(habitId = sid, at = smokingQuit + 9 * DAY_MS, streakStart = smokingQuit),
                Relapse(habitId = sid, at = s.streakStart, streakStart = smokingQuit + 9 * DAY_MS + 2 * hour),
            )
        )
        res.getStringArray(app.unhook.R.array.reasons_smoking).take(3).forEach { dao.insertReason(Reason(habitId = sid, text = it)) }
        dao.insertReward(Reward(habitId = sid, title = if (ru) "Кроссовки для бега" else "Running shoes", price = if (ru) 6000.0 else 120.0, boughtAt = now - 6 * DAY_MS))
        dao.insertReward(Reward(habitId = sid, title = if (ru) "Выходные в Питере" else "Weekend by the sea", price = if (ru) 25000.0 else 600.0))
        dao.insertReward(Reward(habitId = sid, title = if (ru) "Наушники" else "Headphones", price = if (ru) 9000.0 else 250.0))
        listOf(2, 5, 11, 20).forEach { dao.insertEntry(Entry(habitId = sid, at = now - it * DAY_MS - 3 * hour, kind = EntryKind.CRAVING)) }

        val alcoholQuit = now - 19 * DAY_MS - 9 * hour
        val aid = dao.insert(
            Habit(name = "Alcohol", kind = "alcohol", quitAt = alcoholQuit, streakStart = alcoholQuit, times = 3.0, timesPeriod = Period.WEEK,
                money = if (ru) 2500.0 else 60.0, moneyPeriod = Period.WEEK, minutes = 240.0, minutesPeriod = Period.WEEK, createdAt = alcoholQuit + 1)
        )
        res.getStringArray(app.unhook.R.array.reasons_alcohol).take(2).forEach { dao.insertReason(Reason(habitId = aid, text = it)) }

        val socialQuit = now - 4 * DAY_MS - 2 * hour
        dao.insert(
            Habit(name = "Social media", kind = "social_media", quitAt = socialQuit, streakStart = socialQuit, times = 40.0, minutes = 150.0, createdAt = socialQuit + 2)
        )
    }
}

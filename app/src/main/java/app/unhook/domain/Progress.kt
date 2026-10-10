package app.unhook.domain

import app.unhook.data.Habit
import app.unhook.data.Period
import app.unhook.data.Relapse
import app.unhook.data.Reward
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

const val DAY_MS = 24L * 60 * 60 * 1000

/** Trophy ladder. The current goal is always the next rung, so the user never has to pick one. */
enum class Milestone(val days: Int) {
    DAY_1(1), DAY_3(3), WEEK_1(7), WEEK_2(14), MONTH_1(30), MONTH_3(90), MONTH_6(180), YEAR_1(365), YEAR_2(730), YEAR_5(1825);

    val ms: Long get() = days * DAY_MS

    companion object {
        /** The rung being climbed right now, or null once five years are behind. */
        fun next(streakMs: Long): Milestone? = entries.firstOrNull { it.ms > streakMs }

        fun previous(streakMs: Long): Milestone? = entries.lastOrNull { it.ms <= streakMs }

        fun reached(streakMs: Long): List<Milestone> = entries.filter { it.ms <= streakMs }
    }
}

data class Goal(val target: Milestone?, val fraction: Float, val remainingMs: Long)

/** Progress counts from zero, not from the last rung: day 3 of a week reads 43%, not 0%. */
fun goalOf(streakMs: Long): Goal {
    val next = Milestone.next(streakMs) ?: return Goal(null, 1f, 0)
    return Goal(next, (streakMs.toFloat() / next.ms).coerceIn(0f, 1f), next.ms - streakMs)
}

fun perDay(amount: Double, period: Period): Double = if (amount <= 0) 0.0 else amount / period.days

data class Stats(
    val streakMs: Long,
    val totalMs: Long,
    val resets: Int,
    val bestMs: Long,
    val averageMs: Long?,
    val shortestMs: Long?,
    val longestFinishedMs: Long?,
    /** Lengths of finished streaks followed by the current one, oldest first. */
    val streaks: List<Long>,
    val savedSinceReset: Double,
    val savedTotal: Double,
    val spentOnRewards: Double,
    val rewardsBought: Int,
    val avoidedTotal: Double,
    val minutesRegained: Double,
) {
    val balance: Double get() = savedTotal - spentOnRewards
}

fun statsOf(habit: Habit, relapses: List<Relapse>, rewards: List<Reward>, now: Long): Stats {
    val streak = (now - habit.streakStart).coerceAtLeast(0)
    val total = (now - habit.quitAt).coerceAtLeast(0)
    val finished = relapses.map { (it.at - it.streakStart).coerceAtLeast(0) }
    val money = perDay(habit.money, habit.moneyPeriod)
    val times = perDay(habit.times, habit.timesPeriod)
    val minutes = perDay(habit.minutes, habit.minutesPeriod)
    val days = total.toDouble() / DAY_MS
    val bought = rewards.filter { it.boughtAt != null }
    return Stats(
        streakMs = streak,
        totalMs = total,
        resets = relapses.size,
        bestMs = maxOf(streak, finished.maxOrNull() ?: 0),
        averageMs = if (finished.isEmpty()) null else finished.average().toLong(),
        shortestMs = finished.minOrNull(),
        longestFinishedMs = finished.maxOrNull(),
        streaks = finished + streak,
        savedSinceReset = money * streak / DAY_MS,
        savedTotal = money * days,
        spentOnRewards = bought.sumOf { it.price },
        rewardsBought = bought.size,
        avoidedTotal = times * days,
        minutesRegained = minutes * days,
    )
}

enum class DayMark { NONE, CLEAN, RELAPSE, TODAY_CLEAN }

/** How each calendar day looks: green since the quit date, red where the timer was reset. */
fun dayMarks(habit: Habit, relapses: List<Relapse>, zone: ZoneId, today: LocalDate): (LocalDate) -> DayMark {
    val start = Instant.ofEpochMilli(habit.quitAt).atZone(zone).toLocalDate()
    val bad = relapses.map { Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate() }.toSet()
    return { d ->
        when {
            d.isBefore(start) || d.isAfter(today) -> DayMark.NONE
            d in bad -> DayMark.RELAPSE
            d == today -> DayMark.TODAY_CLEAN
            else -> DayMark.CLEAN
        }
    }
}

/** Splits a duration into the largest units for the big live counter. */
data class Parts(val days: Long, val hours: Long, val minutes: Long, val seconds: Long)

fun partsOf(ms: Long): Parts {
    val s = ms.coerceAtLeast(0) / 1000
    return Parts(s / 86_400, s % 86_400 / 3600, s % 3600 / 60, s % 60)
}

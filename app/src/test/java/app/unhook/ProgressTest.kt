package app.unhook

import app.unhook.data.Habit
import app.unhook.data.Period
import app.unhook.data.Relapse
import app.unhook.data.Reward
import app.unhook.domain.DAY_MS
import app.unhook.domain.DayMark
import app.unhook.domain.Milestone
import app.unhook.domain.dayMarks
import app.unhook.domain.goalOf
import app.unhook.domain.perDay
import app.unhook.domain.statsOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ProgressTest {
    private val t0 = LocalDate.of(2026, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun habit(streakStart: Long = t0) = Habit(
        name = "Smoking", kind = "smoking", quitAt = t0, streakStart = streakStart,
        times = 10.0, timesPeriod = Period.DAY, money = 70.0, moneyPeriod = Period.WEEK,
        minutes = 30.0, minutesPeriod = Period.DAY, createdAt = t0,
    )

    @Test
    fun goalClimbsTheLadder() {
        assertEquals(Milestone.DAY_1, goalOf(0).target)
        assertEquals(0.5f, goalOf(DAY_MS / 2).fraction, 0.001f)
        // Exactly one day in: the first trophy is done, and a third of the way to the next.
        assertEquals(Milestone.DAY_3, goalOf(DAY_MS).target)
        assertEquals(1 / 3f, goalOf(DAY_MS).fraction, 0.001f)
        assertEquals(3 / 7f, goalOf(3 * DAY_MS).fraction, 0.001f)
        assertNull(goalOf(1825 * DAY_MS).target)
    }

    @Test
    fun ratesNormaliseToPerDay() {
        assertEquals(10.0, perDay(70.0, Period.WEEK), 1e-9)
        assertEquals(0.0, perDay(-5.0, Period.DAY), 1e-9)
    }

    @Test
    fun statsCountSavingsStreaksAndRewards() {
        val relapses = listOf(
            Relapse(habitId = 1, at = t0 + 2 * DAY_MS, streakStart = t0),
            Relapse(habitId = 1, at = t0 + 6 * DAY_MS, streakStart = t0 + 2 * DAY_MS),
        )
        val rewards = listOf(Reward(habitId = 1, title = "Shoes", price = 50.0, boughtAt = t0), Reward(habitId = 1, title = "Trip", price = 500.0))
        val s = statsOf(habit(streakStart = t0 + 6 * DAY_MS), relapses, rewards, now = t0 + 10 * DAY_MS)

        assertEquals(4 * DAY_MS, s.streakMs)
        assertEquals(2, s.resets)
        assertEquals(4 * DAY_MS, s.bestMs)
        assertEquals(3 * DAY_MS, s.averageMs)
        assertEquals(2 * DAY_MS, s.shortestMs)
        assertEquals(listOf(2 * DAY_MS, 4 * DAY_MS, 4 * DAY_MS), s.streaks)
        assertEquals(40.0, s.savedSinceReset, 1e-6)
        assertEquals(100.0, s.savedTotal, 1e-6)
        assertEquals(50.0, s.balance, 1e-6)
        assertEquals(1, s.rewardsBought)
        assertEquals(100.0, s.avoidedTotal, 1e-6)
        assertEquals(300.0, s.minutesRegained, 1e-6)
    }

    @Test
    fun calendarMarksCleanDaysAndSlips() {
        val relapses = listOf(Relapse(habitId = 1, at = t0 + 2 * DAY_MS + 3600_000, streakStart = t0))
        val today = LocalDate.of(2026, 1, 5)
        val mark = dayMarks(habit(), relapses, ZoneOffset.UTC, today)
        assertEquals(DayMark.NONE, mark(LocalDate.of(2025, 12, 31)))
        assertEquals(DayMark.CLEAN, mark(LocalDate.of(2026, 1, 1)))
        assertEquals(DayMark.RELAPSE, mark(LocalDate.of(2026, 1, 3)))
        assertEquals(DayMark.TODAY_CLEAN, mark(today))
        assertEquals(DayMark.NONE, mark(LocalDate.of(2026, 1, 6)))
    }
}

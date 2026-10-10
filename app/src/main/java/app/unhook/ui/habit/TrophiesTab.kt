package app.unhook.ui.habit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.unhook.R
import app.unhook.domain.Milestone
import app.unhook.ui.components.Bar
import app.unhook.ui.components.HabitIcon
import app.unhook.ui.components.SectionTitle
import app.unhook.ui.components.TrophyBadge
import app.unhook.ui.components.UCard
import app.unhook.ui.dateLabel
import app.unhook.ui.milestoneLabel
import app.unhook.ui.shortDuration
import app.unhook.ui.theme.U
import app.unhook.ui.theme.UType

fun LazyListScope.trophies(d: HabitData) {
    val streak = d.stats.streakMs
    val earned = Milestone.reached(streak).size
    item {
        SectionTitle(pluralStringResource(R.plurals.tr_count, earned, earned, Milestone.entries.size))
        UCard(Modifier.padding(bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HabitIcon(Icons.Rounded.Flag, 56.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.tr_decision), style = UType.Strong)
                    Text(dateLabel(d.habit.quitAt), style = UType.Muted)
                }
            }
        }
    }
    items(Milestone.entries, key = { it.name }) { m ->
        val got = streak >= m.ms
        val at = d.habit.streakStart + m.ms
        // Trophies already won in an earlier streak stay visible as "personal best".
        val before = !got && d.stats.bestMs >= m.ms
        UCard(Modifier.padding(bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TrophyBadge(m, got, short(m), 72.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(milestoneLabel(m), style = UType.Strong)
                    Spacer(Modifier.height(4.dp))
                    when {
                        got -> Text(stringResource(R.string.tr_earned, dateLabel(at)), style = UType.Muted.copy(color = U.c.accent))
                        else -> {
                            Text(stringResource(R.string.tr_left, shortDuration(m.ms - streak), dateLabel(at)), style = UType.Muted)
                            Spacer(Modifier.height(8.dp))
                            Bar((streak.toFloat() / m.ms))
                            if (before) {
                                Spacer(Modifier.height(6.dp))
                                Text(stringResource(R.string.tr_before), style = UType.Small)
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun short(m: Milestone): String = stringResource(
    when (m) {
        Milestone.DAY_1 -> R.string.ms_1d
        Milestone.DAY_3 -> R.string.ms_3d
        Milestone.WEEK_1 -> R.string.ms_1w
        Milestone.WEEK_2 -> R.string.ms_2w
        Milestone.MONTH_1 -> R.string.ms_1m
        Milestone.MONTH_3 -> R.string.ms_3m
        Milestone.MONTH_6 -> R.string.ms_6m
        Milestone.YEAR_1 -> R.string.ms_1y
        Milestone.YEAR_2 -> R.string.ms_2y
        Milestone.YEAR_5 -> R.string.ms_5y
    }
)

package app.unhook.ui.habit

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.HourglassFull
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.unhook.R
import app.unhook.data.EntryKind
import app.unhook.ui.components.InfoRow
import app.unhook.ui.components.SectionTitle
import app.unhook.ui.components.StreakChart
import app.unhook.ui.components.UCard
import app.unhook.ui.count
import app.unhook.ui.dateLabel
import app.unhook.ui.minutesLabel
import app.unhook.ui.money
import app.unhook.ui.shortDuration
import app.unhook.ui.theme.U
import app.unhook.ui.theme.UType

fun LazyListScope.stats(d: HabitData) {
    val s = d.stats
    val h = d.habit
    item {
        SectionTitle(stringResource(R.string.st_progress))
        UCard {
            InfoRow(Icons.Rounded.Event, stringResource(R.string.st_quit_day), dateLabel(h.quitAt))
            InfoRow(Icons.Rounded.WorkspacePremium, stringResource(R.string.st_best), shortDuration(s.bestMs), U.c.gold, U.c.goldSoft)
            InfoRow(Icons.Rounded.RestartAlt, stringResource(R.string.st_resets), s.resets.toString(), U.c.danger, U.c.dangerSoft)
            InfoRow(Icons.Rounded.HourglassBottom, stringResource(R.string.st_average), s.averageMs?.let { shortDuration(it) } ?: "—")
            InfoRow(Icons.Rounded.HourglassEmpty, stringResource(R.string.st_min), s.shortestMs?.let { shortDuration(it) } ?: "—")
            InfoRow(Icons.Rounded.HourglassFull, stringResource(R.string.st_max), s.longestFinishedMs?.let { shortDuration(it) } ?: "—")
            val cravings = d.entries.count { it.kind == EntryKind.CRAVING }
            InfoRow(Icons.Rounded.Shield, stringResource(R.string.st_cravings), cravings.toString(), U.c.sky, U.c.skySoft)
        }
    }
    if (s.streaks.size > 1) {
        item {
            SectionTitle(stringResource(R.string.st_chart))
            UCard {
                StreakChart(s.streaks)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.st_chart_note), style = UType.Small)
            }
        }
    }
    if (h.money > 0 || h.minutes > 0 || h.times > 0) {
        item {
            SectionTitle(stringResource(R.string.st_gained))
            UCard {
                if (h.money > 0) {
                    InfoRow(Icons.Rounded.Payments, stringResource(R.string.rw_total), money(s.savedTotal, d.currency), U.c.gold, U.c.goldSoft)
                    InfoRow(Icons.Rounded.CardGiftcard, stringResource(R.string.st_rewards_bought), s.rewardsBought.toString(), U.c.gold, U.c.goldSoft)
                    InfoRow(Icons.Rounded.CardGiftcard, stringResource(R.string.st_rewards_spent), money(s.spentOnRewards, d.currency), U.c.gold, U.c.goldSoft)
                }
                if (h.minutes > 0) InfoRow(Icons.Rounded.Schedule, stringResource(R.string.rw_time_back), minutesLabel(s.minutesRegained))
                if (h.times > 0) InfoRow(Icons.Rounded.Block, stringResource(R.string.rw_avoided), count(s.avoidedTotal))
            }
        }
    }
}

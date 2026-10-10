package app.unhook.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.unhook.R
import app.unhook.container
import app.unhook.data.Habit
import app.unhook.domain.goalOf
import app.unhook.domain.partsOf
import app.unhook.ui.Preset
import app.unhook.ui.components.Bar
import app.unhook.ui.components.HabitIcon
import app.unhook.ui.components.UCard
import app.unhook.ui.components.rememberNow
import app.unhook.ui.habitName
import app.unhook.ui.iconVector
import app.unhook.ui.milestoneLabel
import app.unhook.ui.quoteOfDay
import app.unhook.ui.theme.U
import app.unhook.ui.theme.UType
import java.time.LocalDate

@Composable
fun HomeScreen(onOpen: (Long) -> Unit, onAdd: () -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    val habits by context.container.db.habits().observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val now = rememberNow()
    var shift by rememberSaveable { mutableIntStateOf(0) }
    val quote = quoteOfDay(LocalDate.now(), shift)

    Box(Modifier.fillMaxSize().background(U.c.bg)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.app_name), style = UType.Title, modifier = Modifier.weight(1f))
                    IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = U.c.muted) }
                }
            }
            item {
                // Tap for another quote: Quitzilla hides this behind a shake gesture.
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(U.RLg)
                        .background(U.c.accentSoft)
                        .clickable { shift++ }
                        .padding(20.dp),
                ) {
                    Icon(Icons.Rounded.FormatQuote, null, tint = U.c.accent, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(quote.text, style = UType.Body.copy(fontSize = 17.sp, lineHeight = 25.sp))
                    if (quote.author.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("— " + quote.author, style = UType.Muted)
                    }
                }
            }
            if (habits.isNotEmpty()) {
                item { Text(stringResource(R.string.home_committed).uppercase(), style = UType.Eyebrow, modifier = Modifier.padding(start = 4.dp, top = 10.dp)) }
            }
            if (habits.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.home_empty),
                        style = UType.Sub,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
                    )
                }
            }
            items(habits, key = { it.id }) { h -> HabitCard(h, now) { onOpen(h.id) } }
        }
        ExtendedFloatingActionButton(
            onClick = onAdd,
            containerColor = U.c.accent,
            contentColor = U.c.onAccent,
            shape = U.RMd,
            icon = { Icon(Icons.Rounded.Add, null) },
            text = { Text(stringResource(R.string.home_add), style = UType.Button) },
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp),
        )
    }
}

@Composable
private fun HabitCard(h: Habit, now: Long, onClick: () -> Unit) {
    val streak = now - h.streakStart
    val goal = goalOf(streak)
    val p = partsOf(streak)
    UCard(onClick = onClick, padding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HabitIcon(h.iconVector, 46.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(habitName(h.name), style = UType.Strong)
                Text(stringResource(R.string.home_clean_for), style = UType.Muted)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Part(p.days, stringResource(R.string.u_d))
            Part(p.hours, stringResource(R.string.u_h))
            Part(p.minutes, stringResource(R.string.u_m))
            Part(p.seconds, stringResource(R.string.u_s), faint = true)
        }
        Spacer(Modifier.height(14.dp))
        Row {
            Text(
                goal.target?.let { stringResource(R.string.home_next, milestoneLabel(it)) } ?: stringResource(R.string.home_all_done),
                style = UType.Muted,
                modifier = Modifier.weight(1f),
            )
            Text("${(goal.fraction * 100).toInt()}%", style = UType.Muted)
        }
        Spacer(Modifier.height(8.dp))
        Bar(goal.fraction)
    }
}

@Composable
private fun Part(value: Long, unit: String, faint: Boolean = false) {
    Text(value.toString(), style = UType.Number.copy(fontSize = 30.sp, color = if (faint) U.c.faint else U.c.ink))
    Text(unit, style = UType.Muted.copy(fontSize = 15.sp), modifier = Modifier.padding(start = 2.dp, end = 12.dp, bottom = 4.dp))
}

package app.unhook.ui.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unhook.R
import app.unhook.container
import app.unhook.data.Entry
import app.unhook.data.EntryKind
import app.unhook.data.Relapse
import app.unhook.domain.dayMarks
import app.unhook.domain.goalOf
import app.unhook.domain.partsOf
import app.unhook.ui.components.ConfirmDialog
import app.unhook.ui.components.MonthCalendar
import app.unhook.ui.components.ProgressRing
import app.unhook.ui.components.SectionTitle
import app.unhook.ui.components.SoftButton
import app.unhook.ui.components.UCard
import app.unhook.ui.dateTimeLabel
import app.unhook.ui.milestoneLabel
import app.unhook.ui.reasonText
import app.unhook.ui.shortDuration
import app.unhook.ui.theme.U
import app.unhook.ui.theme.UType
import java.time.LocalDate
import java.time.ZoneId

val moods = listOf("😣", "😕", "😐", "🙂", "😄")

fun LazyListScope.overview(d: HabitData, a: HabitActions) {
    item {
        val streak = d.stats.streakMs
        val goal = goalOf(streak)
        UCard {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                ProgressRing(goal.fraction, Modifier.size(230.dp), stroke = 14.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val p = partsOf(streak)
                        Text(p.days.toString(), style = UType.Display.copy(fontSize = 64.sp, lineHeight = 66.sp))
                        Text(stringResource(R.string.ov_days_clean), style = UType.Muted)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "%02d:%02d:%02d".format(p.hours, p.minutes, p.seconds),
                            style = UType.Strong.copy(fontSize = 18.sp, color = U.c.muted),
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            if (goal.target != null) {
                Text(
                    stringResource(R.string.ov_goal, milestoneLabel(goal.target), (goal.fraction * 100).toInt()),
                    style = UType.Strong,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.ov_goal_left, shortDuration(goal.remainingMs, seconds = true)),
                    style = UType.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(stringResource(R.string.home_all_done), style = UType.Strong, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            if (d.reasons.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                val reason = d.reasons[((d.now / 86_400_000L) % d.reasons.size).toInt()]
                Text(
                    "“${reasonText(reason.text)}”",
                    style = UType.Sub.copy(fontSize = 15.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }
    item {
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SoftButton(stringResource(R.string.ov_craving), { a.open(Sheet.CRAVING) }, Modifier.weight(1f), icon = Icons.Rounded.Air, color = U.c.sky, soft = U.c.skySoft)
            SoftButton(stringResource(R.string.ov_relapse), { a.open(Sheet.RELAPSE) }, Modifier.weight(1f), icon = Icons.Rounded.RestartAlt, color = U.c.danger, soft = U.c.dangerSoft)
        }
    }
    item {
        SectionTitle(stringResource(R.string.ov_calendar))
        UCard {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val mark = remember(d.habit, d.relapses, today) { dayMarks(d.habit, d.relapses, zone, today) }
            MonthCalendar(mark, today)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Legend(U.c.accentSoft, stringResource(R.string.ov_legend_clean))
                Spacer(Modifier.width(18.dp))
                Legend(U.c.dangerSoft, stringResource(R.string.ov_legend_relapse))
            }
        }
    }
    item {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(R.string.ov_diary), Modifier.weight(1f))
        }
        SoftButton(stringResource(R.string.ov_add_note), { a.open(Sheet.NOTE) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.EditNote)
        Spacer(Modifier.height(10.dp))
    }
    val latest = d.relapses.maxByOrNull { it.at }
    val timeline = (d.entries.map { DiaryItem.Note(it) } + d.relapses.map { DiaryItem.Slip(it, undoable = it == latest) })
        .sortedByDescending { it.at }
    if (timeline.isEmpty()) {
        item { Text(stringResource(R.string.ov_diary_empty), style = UType.Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(16.dp)) }
    }
    items(timeline, key = { it.key }) { DiaryRow(it) }
}

private sealed class DiaryItem(val at: Long, val key: String) {
    class Note(val entry: Entry) : DiaryItem(entry.at, "e${entry.id}")
    class Slip(val relapse: Relapse, val undoable: Boolean) : DiaryItem(relapse.at, "r${relapse.id}") {
        val note get() = relapse.note
        val streakMs get() = relapse.at - relapse.streakStart
    }
}

@Composable
private fun DiaryRow(item: DiaryItem) {
    val context = LocalContext.current
    var confirm by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val (icon, tint, soft) = when {
        item is DiaryItem.Slip -> Triple(Icons.Rounded.RestartAlt, U.c.danger, U.c.dangerSoft)
        (item as DiaryItem.Note).entry.kind == EntryKind.CRAVING -> Triple(Icons.Rounded.Shield, U.c.sky, U.c.skySoft)
        else -> Triple(Icons.Rounded.EditNote, U.c.accent, U.c.accentSoft)
    }
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(U.RMd).background(U.c.surface).padding(14.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(soft), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(19.dp), tint = tint)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            val title = when (item) {
                is DiaryItem.Slip -> stringResource(R.string.diary_slip, shortDuration(item.streakMs))
                is DiaryItem.Note -> if (item.entry.kind == EntryKind.CRAVING) stringResource(R.string.diary_craving) else stringResource(R.string.diary_note)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = UType.Strong.copy(fontSize = 15.sp), modifier = Modifier.weight(1f, fill = false))
                val mood = (item as? DiaryItem.Note)?.entry?.mood
                if (mood != null) Text("  " + moods[mood - 1], fontSize = 16.sp)
            }
            Text(dateTimeLabel(item.at), style = UType.Small)
            val text = when (item) {
                is DiaryItem.Slip -> item.note
                is DiaryItem.Note -> item.entry.text
            }
            if (text.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(text, style = UType.Body)
            }
        }
        if (item is DiaryItem.Note || (item is DiaryItem.Slip && item.undoable)) {
            IconButton(onClick = { confirm = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.delete), tint = U.c.faint, modifier = Modifier.size(19.dp))
            }
        }
    }
    if (confirm && item is DiaryItem.Slip) {
        ConfirmDialog(
            title = stringResource(R.string.diary_undo_title),
            text = stringResource(R.string.diary_undo_text, shortDuration(item.streakMs)),
            confirm = stringResource(R.string.diary_undo),
            onConfirm = { context.container.write { habits().undoRelapse(item.relapse) } },
            onDismiss = { confirm = false },
        )
    }
    if (confirm && item is DiaryItem.Note) {
        ConfirmDialog(
            title = stringResource(R.string.diary_delete_title),
            text = stringResource(R.string.diary_delete_text),
            confirm = stringResource(R.string.delete),
            danger = true,
            onConfirm = { context.container.write { habits().deleteEntry(item.entry) } },
            onDismiss = { confirm = false },
        )
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = UType.Small)
    }
}

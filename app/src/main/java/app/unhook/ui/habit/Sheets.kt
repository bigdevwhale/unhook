package app.unhook.ui.habit

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unhook.R
import app.unhook.container
import app.unhook.data.Entry
import app.unhook.data.EntryKind
import app.unhook.data.Reason
import app.unhook.data.Reward
import app.unhook.ui.components.Chip
import app.unhook.ui.components.DateTimePickerDialog
import app.unhook.ui.components.PrimaryButton
import app.unhook.ui.components.SoftButton
import app.unhook.ui.components.UTextField
import app.unhook.ui.components.parseAmount
import app.unhook.ui.dateTimeLabel
import app.unhook.ui.money
import app.unhook.ui.reasonText
import app.unhook.ui.shortDuration
import app.unhook.ui.theme.U
import app.unhook.ui.theme.UType
import kotlinx.coroutines.delay

@Composable
fun HabitSheets(d: HabitData, sheet: Sheet?, reason: Reason?, onClose: () -> Unit, onSwitch: (Sheet) -> Unit) {
    when (sheet) {
        Sheet.CRAVING -> CravingSheet(d, onClose, onSlipped = { onSwitch(Sheet.RELAPSE) })
        Sheet.RELAPSE -> RelapseSheet(d, onClose)
        Sheet.NOTE -> NoteSheet(d, onClose)
        Sheet.REWARD -> RewardSheet(d, onClose)
        Sheet.REASON -> ReasonSheet(d, reason, onClose)
        null -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Sheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = U.c.bg,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) { content() }
    }
}

private enum class Breath(val seconds: Int, val label: Int, val target: Float) {
    IN(4, R.string.sos_in, 1f), HOLD(4, R.string.sos_hold, 1f), OUT(6, R.string.sos_out, 0.55f)
}

/**
 * New compared to Quitzilla: an urge usually peaks and fades within minutes, so we give the
 * user something to do with those minutes (paced breathing + their own reasons) and count the win.
 */
@Composable
private fun CravingSheet(d: HabitData, onDismiss: () -> Unit, onSlipped: () -> Unit) {
    val context = LocalContext.current
    var phase by remember { mutableStateOf(Breath.IN) }
    var cycles by remember { mutableIntStateOf(0) }
    val scale = remember { Animatable(0.55f) }
    LaunchedEffect(Unit) {
        while (true) {
            for (b in Breath.entries) {
                phase = b
                // Holding keeps the size, so animateTo would return at once: wait it out instead.
                if (scale.value == b.target) delay(b.seconds * 1000L)
                else scale.animateTo(b.target, tween(b.seconds * 1000, easing = LinearEasing))
            }
            cycles++
        }
    }
    val passed = stringResource(R.string.sos_saved)
    Sheet(onDismiss) {
        Text(stringResource(R.string.sos_title), style = UType.Title.copy(fontSize = 23.sp, lineHeight = 28.sp))
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.sos_sub), style = UType.Sub)
        Box(Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(220.dp).scale(scale.value).clip(CircleShape).background(U.c.skySoft))
            Box(Modifier.size(150.dp).scale(scale.value).clip(CircleShape).background(U.c.sky.copy(alpha = 0.25f)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(phase.label), style = UType.Strong.copy(fontSize = 20.sp, color = U.c.sky))
                Text(stringResource(R.string.sos_cycles, cycles), style = UType.Small)
            }
        }
        if (d.reasons.isNotEmpty()) {
            Text(stringResource(R.string.sos_remember).uppercase(), style = UType.Eyebrow)
            Spacer(Modifier.height(8.dp))
            d.reasons.take(3).forEach {
                Text("• " + reasonText(it.text), style = UType.Body, modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
        Text(stringResource(R.string.sos_streak, shortDuration(d.stats.streakMs)), style = UType.Muted)
        Spacer(Modifier.height(16.dp))
        PrimaryButton(stringResource(R.string.sos_passed), {
            context.container.write { habits().insertEntry(Entry(habitId = d.habit.id, at = System.currentTimeMillis(), kind = EntryKind.CRAVING)) }
            Toast.makeText(context, passed, Toast.LENGTH_SHORT).show()
            onDismiss()
        }, Modifier.fillMaxWidth(), color = U.c.sky)
        TextButton(onClick = onSlipped, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sos_slipped), color = U.c.muted, style = UType.Button)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelapseSheet(d: HabitData, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var now by rememberSaveable { mutableStateOf(true) }
    var at by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var note by rememberSaveable { mutableStateOf("") }
    var pick by rememberSaveable { mutableStateOf(false) }
    Sheet(onDismiss) {
        Text(stringResource(R.string.rl_title), style = UType.Title.copy(fontSize = 23.sp, lineHeight = 28.sp))
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.rl_sub, shortDuration(d.stats.streakMs)), style = UType.Sub)
        Spacer(Modifier.height(16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(stringResource(R.string.rl_now), now, { now = true })
            Chip(stringResource(R.string.rl_earlier), !now, { now = false; pick = true })
        }
        if (!now) SoftButton(dateTimeLabel(at), { pick = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Event)
        Spacer(Modifier.height(10.dp))
        UTextField(note, { note = it }, placeholder = stringResource(R.string.rl_note_hint), minLines = 2)
        Spacer(Modifier.height(16.dp))
        PrimaryButton(stringResource(R.string.rl_confirm), {
            val time = if (now) System.currentTimeMillis() else at.coerceAtLeast(d.habit.streakStart)
            context.container.write { habits().relapse(d.habit, time, note.trim()) }
            onDismiss()
        }, Modifier.fillMaxWidth(), color = U.c.danger)
    }
    if (pick) DateTimePickerDialog(at, onDismiss = { pick = false }) { at = it }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteSheet(d: HabitData, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    var mood by rememberSaveable { mutableStateOf<Int?>(null) }
    Sheet(onDismiss) {
        Text(stringResource(R.string.note_title), style = UType.Title.copy(fontSize = 23.sp, lineHeight = 28.sp))
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            moods.forEachIndexed { i, e ->
                val on = mood == i + 1
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(if (on) U.c.accentSoft else U.c.surface).clickable { mood = if (on) null else i + 1 },
                    contentAlignment = Alignment.Center,
                ) { Text(e, fontSize = if (on) 28.sp else 24.sp) }
            }
        }
        Spacer(Modifier.height(16.dp))
        UTextField(text, { text = it }, placeholder = stringResource(R.string.note_hint), minLines = 4)
        Spacer(Modifier.height(16.dp))
        PrimaryButton(stringResource(R.string.save), {
            context.container.write { habits().insertEntry(Entry(habitId = d.habit.id, at = System.currentTimeMillis(), kind = EntryKind.NOTE, text = text.trim(), mood = mood)) }
            onDismiss()
        }, Modifier.fillMaxWidth(), enabled = text.isNotBlank() || mood != null)
    }
}

@Composable
private fun RewardSheet(d: HabitData, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    val p = parseAmount(price)
    Sheet(onDismiss) {
        Text(stringResource(R.string.rw_add_title), style = UType.Title.copy(fontSize = 23.sp, lineHeight = 28.sp))
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.rw_add_sub, money(d.stats.balance, d.currency)), style = UType.Sub)
        Spacer(Modifier.height(16.dp))
        UTextField(title, { title = it }, placeholder = stringResource(R.string.rw_title_hint))
        Spacer(Modifier.height(10.dp))
        UTextField(price, { price = it }, placeholder = stringResource(R.string.rw_price_hint), number = true)
        Spacer(Modifier.height(16.dp))
        PrimaryButton(stringResource(R.string.save), {
            context.container.write { habits().insertReward(Reward(habitId = d.habit.id, title = title.trim(), price = p ?: 0.0)) }
            onDismiss()
        }, Modifier.fillMaxWidth(), enabled = title.isNotBlank() && p != null && p > 0, color = U.c.gold)
    }
}

@Composable
private fun ReasonSheet(d: HabitData, reason: Reason?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val stock = reason?.let { reasonText(it.text) } ?: ""
    var text by rememberSaveable { mutableStateOf(stock) }
    Sheet(onDismiss) {
        Text(stringResource(if (reason == null) R.string.mo_add_reason else R.string.mo_edit_reason), style = UType.Title.copy(fontSize = 23.sp, lineHeight = 28.sp))
        Spacer(Modifier.height(16.dp))
        UTextField(text, { text = it }, placeholder = stringResource(R.string.mo_reason_hint), minLines = 2)
        Spacer(Modifier.height(16.dp))
        PrimaryButton(stringResource(R.string.save), {
            val t = text.trim()
            context.container.write {
                if (reason == null) habits().insertReason(Reason(habitId = d.habit.id, text = t)) else habits().updateReason(reason.copy(text = t))
            }
            onDismiss()
        }, Modifier.fillMaxWidth(), enabled = text.isNotBlank())
        if (reason != null) {
            TextButton(onClick = {
                context.container.write { habits().deleteReason(reason) }
                onDismiss()
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.delete), color = U.c.danger, style = UType.Button) }
        }
    }
}

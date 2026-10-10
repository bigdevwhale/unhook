package app.unhook.ui.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.unhook.R
import app.unhook.container
import app.unhook.data.Habit
import app.unhook.data.Period
import app.unhook.ui.components.ConfirmDialog
import app.unhook.ui.components.DateTimePickerDialog
import app.unhook.ui.components.IconPicker
import app.unhook.ui.components.PrimaryButton
import app.unhook.ui.components.SectionTitle
import app.unhook.ui.components.Segmented
import app.unhook.ui.components.SoftButton
import app.unhook.ui.components.TopBar
import app.unhook.ui.components.UTextField
import app.unhook.ui.components.formatAmount
import app.unhook.ui.components.parseAmount
import app.unhook.ui.dateTimeLabel
import app.unhook.ui.habitName
import app.unhook.ui.periodLabel
import app.unhook.ui.theme.U
import app.unhook.ui.theme.UType
import java.util.Currency

@Composable
fun EditHabitScreen(id: Long, currency: String, onDone: () -> Unit, onDeleted: () -> Unit) {
    val context = LocalContext.current
    // Habit is not saveable itself; the editable fields below are, and they outlive a reload.
    var habit by remember { mutableStateOf<Habit?>(null) }
    LaunchedEffect(id) { if (habit == null) habit = context.container.db.habits().get(id) }
    val h = habit ?: return

    val shownName = habitName(h.name)
    var name by rememberSaveable { mutableStateOf(shownName) }
    var icon by rememberSaveable { mutableStateOf(h.icon.ifEmpty { h.kind }) }
    var quitAt by rememberSaveable { mutableLongStateOf(h.quitAt) }
    var streakStart by rememberSaveable { mutableLongStateOf(h.streakStart) }
    var times by rememberSaveable { mutableStateOf(formatAmount(h.times)) }
    var timesPeriod by rememberSaveable { mutableStateOf(h.timesPeriod) }
    var money by rememberSaveable { mutableStateOf(formatAmount(h.money)) }
    var moneyPeriod by rememberSaveable { mutableStateOf(h.moneyPeriod) }
    var minutes by rememberSaveable { mutableStateOf(formatAmount(h.minutes)) }
    var minutesPeriod by rememberSaveable { mutableStateOf(h.minutesPeriod) }
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val valid = name.isNotBlank() &&
        (times.isBlank() || parseAmount(times) != null) &&
        (money.isBlank() || parseAmount(money) != null) &&
        (minutes.isBlank() || parseAmount(minutes) != null)

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        TopBar(stringResource(R.string.edit_title), onDone)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            SectionTitle(stringResource(R.string.edit_name))
            UTextField(name, { name = it })
            SectionTitle(stringResource(R.string.edit_icon))
            IconPicker(icon, { icon = it })

            SectionTitle(stringResource(R.string.edit_dates))
            DateRow(stringResource(R.string.edit_quit_at), dateTimeLabel(quitAt)) { picking = "quit" }
            Spacer(Modifier.height(8.dp))
            DateRow(stringResource(R.string.edit_streak_start), dateTimeLabel(streakStart)) { picking = "streak" }

            SectionTitle(stringResource(R.string.edit_times))
            UTextField(times, { times = it }, placeholder = "0", number = true, suffix = stringResource(R.string.times_suffix))
            Spacer(Modifier.height(8.dp))
            Segmented(Period.entries, timesPeriod, { stringResource(R.string.per, periodLabel(it)) }, { timesPeriod = it })

            SectionTitle(stringResource(R.string.edit_money))
            val symbol = runCatching { Currency.getInstance(currency).symbol }.getOrDefault(currency)
            UTextField(money, { money = it }, placeholder = "0", number = true, suffix = symbol)
            Spacer(Modifier.height(8.dp))
            Segmented(Period.entries, moneyPeriod, { stringResource(R.string.per, periodLabel(it)) }, { moneyPeriod = it })

            SectionTitle(stringResource(R.string.edit_minutes))
            UTextField(minutes, { minutes = it }, placeholder = "0", number = true, suffix = stringResource(R.string.u_min))
            Spacer(Modifier.height(8.dp))
            Segmented(Period.entries, minutesPeriod, { stringResource(R.string.per, periodLabel(it)) }, { minutesPeriod = it })

            Spacer(Modifier.height(24.dp))
            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = U.c.danger)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.edit_delete), color = U.c.danger, style = UType.Button)
            }
            Spacer(Modifier.height(12.dp))
        }
        PrimaryButton(stringResource(R.string.save), {
            val updated = h.copy(
                name = name.trim(),
                icon = if (icon == h.kind) "" else icon,
                quitAt = minOf(quitAt, streakStart),
                streakStart = streakStart,
                times = parseAmount(times) ?: 0.0,
                timesPeriod = timesPeriod,
                money = parseAmount(money) ?: 0.0,
                moneyPeriod = moneyPeriod,
                minutes = parseAmount(minutes) ?: 0.0,
                minutesPeriod = minutesPeriod,
            )
            context.container.write { habits().update(updated) }
            onDone()
        }, Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), enabled = valid)
    }

    when (picking) {
        "quit" -> DateTimePickerDialog(quitAt, onDismiss = { picking = null }) {
            quitAt = it
            if (streakStart < it) streakStart = it
        }
        "streak" -> DateTimePickerDialog(streakStart, onDismiss = { picking = null }) {
            streakStart = it
            if (quitAt > it) quitAt = it
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.edit_delete_title, shownName),
            text = stringResource(R.string.edit_delete_text),
            confirm = stringResource(R.string.delete),
            danger = true,
            onConfirm = {
                context.container.write { habits().delete(h) }
                onDeleted()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun DateRow(label: String, value: String, onClick: () -> Unit) {
    Column {
        Text(label, style = UType.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        SoftButton(value, onClick, Modifier.fillMaxWidth(), icon = Icons.Rounded.Event, color = U.c.ink, soft = U.c.surface)
    }
}

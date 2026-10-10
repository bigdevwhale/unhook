package app.unhook.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unhook.R
import app.unhook.container
import app.unhook.data.Habit
import app.unhook.data.Period
import app.unhook.data.Reason
import app.unhook.ui.HabitIcons
import app.unhook.ui.Preset
import app.unhook.ui.components.Chip
import app.unhook.ui.components.DateTimePickerDialog
import app.unhook.ui.components.HabitIcon
import app.unhook.ui.components.IconPicker
import app.unhook.ui.components.PrimaryButton
import app.unhook.ui.components.Segmented
import app.unhook.ui.components.SoftButton
import app.unhook.ui.components.UTextField
import app.unhook.ui.components.parseAmount
import app.unhook.ui.dateTimeLabel
import app.unhook.ui.periodLabel
import app.unhook.ui.theme.U
import app.unhook.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Currency

private enum class Step { WELCOME, HABIT, REASONS, TIMES, MONEY, TIME, START }

/**
 * Same questions Quitzilla asks, but numbers are typed inline (no extra dialogs), the
 * cost questions can be skipped, and the goal is not asked at all: it climbs automatically.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(first: Boolean, currency: String, onCancel: (() -> Unit)?, onCreated: (Long) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val steps = remember(first) { Step.entries.filter { first || it != Step.WELCOME } }
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[index]

    var presetKey by rememberSaveable { mutableStateOf<String?>(null) }
    var customName by rememberSaveable { mutableStateOf("") }
    var customIcon by rememberSaveable { mutableStateOf(Preset.CUSTOM.key) }
    var showMore by rememberSaveable { mutableStateOf(false) }
    val picked = remember { mutableStateListOf<String>() }
    var ownReason by rememberSaveable { mutableStateOf("") }
    var times by rememberSaveable { mutableStateOf("") }
    var timesPeriod by rememberSaveable { mutableStateOf(Period.DAY) }
    var money by rememberSaveable { mutableStateOf("") }
    var moneyPeriod by rememberSaveable { mutableStateOf(Period.DAY) }
    var hours by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("") }
    var timePeriod by rememberSaveable { mutableStateOf(Period.DAY) }
    var startNow by rememberSaveable { mutableStateOf(true) }
    var startAt by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var saving by rememberSaveable { mutableStateOf(false) }

    val preset = presetKey?.let { Preset.of(it) }
    val presetLabel = preset?.let { stringResource(it.label) } ?: ""
    val name = if (preset == Preset.CUSTOM) customName.trim() else presetLabel
    val suggestions = preset?.let { stringArrayResource(it.reasons).toList() } ?: emptyList()

    val back: () -> Unit = { if (index > 0) index-- else onCancel?.invoke() }
    BackHandler(enabled = index > 0 || onCancel != null, onBack = back)

    val canContinue = when (step) {
        Step.HABIT -> preset != null && name.isNotEmpty()
        Step.TIMES -> times.isBlank() || parseAmount(times) != null
        Step.MONEY -> money.isBlank() || parseAmount(money) != null
        Step.TIME -> (hours.isBlank() || parseAmount(hours) != null) && (minutes.isBlank() || parseAmount(minutes) != null)
        else -> !saving
    }
    val optional = step in setOf(Step.TIMES, Step.MONEY, Step.TIME)

    fun finish() {
        if (saving) return
        saving = true
        val now = System.currentTimeMillis()
        val start = if (startNow) now else startAt
        val habit = Habit(
            name = name,
            kind = preset!!.key,
            quitAt = start,
            streakStart = start,
            times = parseAmount(times) ?: 0.0,
            timesPeriod = timesPeriod,
            money = parseAmount(money) ?: 0.0,
            moneyPeriod = moneyPeriod,
            minutes = (parseAmount(hours) ?: 0.0) * 60 + (parseAmount(minutes) ?: 0.0),
            minutesPeriod = timePeriod,
            createdAt = now,
            icon = if (preset == Preset.CUSTOM) customIcon else "",
        )
        val reasons = picked.toList()
        val c = context.container
        scope.launch {
            val id = withContext(Dispatchers.IO) {
                val id = c.db.habits().insert(habit)
                reasons.forEach { c.db.habits().insertReason(Reason(habitId = id, text = it)) }
                c.changed()
                id
            }
            onCreated(id)
        }
    }

    val next: () -> Unit = {
        if (step == Step.HABIT && picked.isEmpty() && suggestions.isNotEmpty()) picked.add(suggestions.first())
        if (index < steps.lastIndex) index++ else finish()
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        if (step != Step.WELCOME) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (index > 0 || onCancel != null) {
                    IconButton(onClick = back) {
                        Icon(if (index == 0) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = U.c.ink)
                    }
                } else Spacer(Modifier.width(48.dp))
                LinearProgressIndicator(
                    progress = { index.toFloat() / steps.lastIndex },
                    modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                    color = U.c.accent,
                    trackColor = U.c.line,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
                Spacer(Modifier.width(48.dp))
            }
        }

        AnimatedContent(
            targetState = index,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally { it / 6 * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it / 6 * dir } + fadeOut())
            },
            label = "step",
        ) { i ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {
                when (steps[i]) {
                    Step.WELCOME -> Welcome()
                    Step.HABIT -> {
                        Header(stringResource(R.string.ob_habit_title), stringResource(R.string.ob_habit_sub))
                        val list = if (showMore) Preset.entries.toList() else Preset.featured
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 2) {
                            list.forEach { p ->
                                PresetTile(p, selected = presetKey == p.key, modifier = Modifier.weight(1f)) {
                                    if (presetKey != p.key) picked.clear()
                                    presetKey = p.key
                                }
                            }
                            if (list.size % 2 == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        if (!showMore) {
                            SoftButton(stringResource(R.string.ob_more), { showMore = true }, Modifier.fillMaxWidth(), color = U.c.ink, soft = U.c.line)
                        }
                        if (preset == Preset.CUSTOM) {
                            Spacer(Modifier.height(14.dp))
                            UTextField(customName, { customName = it }, placeholder = stringResource(R.string.ob_custom_hint))
                            Text(stringResource(R.string.ob_custom_icon), style = UType.Muted, modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 10.dp))
                            IconPicker(customIcon, { customIcon = it })
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    Step.REASONS -> {
                        if (preset != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 18.dp)) {
                                HabitIcon(if (preset == Preset.CUSTOM) HabitIcons.of(customIcon) ?: preset.icon else preset.icon, 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(name, style = UType.Strong)
                            }
                        }
                        Header(stringResource(R.string.ob_reasons_title), stringResource(R.string.ob_reasons_sub), top = 18.dp)
                        (suggestions + picked.filter { it !in suggestions }).forEach { r ->
                            val on = r in picked
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                                    .clip(U.RMd)
                                    .background(if (on) U.c.accentSoft else U.c.surface)
                                    .border(1.5.dp, if (on) U.c.accent else U.c.line, U.RMd)
                                    .clickable { if (on) picked.remove(r) else picked.add(r) }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val box = androidx.compose.foundation.shape.RoundedCornerShape(7.dp)
                                Box(
                                    Modifier.size(24.dp).clip(box).background(if (on) U.c.accent else U.c.surface2).border(1.dp, if (on) U.c.accent else U.c.line, box),
                                    contentAlignment = Alignment.Center,
                                ) { if (on) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = U.c.onAccent) }
                                Spacer(Modifier.width(14.dp))
                                Text(r, style = UType.Body)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UTextField(ownReason, { ownReason = it }, Modifier.weight(1f), placeholder = stringResource(R.string.ob_own_reason))
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    val t = ownReason.trim()
                                    if (t.isNotEmpty() && t !in picked) picked.add(t)
                                    ownReason = ""
                                },
                                enabled = ownReason.isNotBlank(),
                            ) { Icon(Icons.Rounded.Add, stringResource(R.string.add), tint = if (ownReason.isNotBlank()) U.c.accent else U.c.faint) }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    Step.TIMES -> {
                        Illustration(Icons.Rounded.Event, U.c.sky, U.c.skySoft)
                        Header(stringResource(R.string.ob_times_title), stringResource(R.string.ob_times_sub), center = true)
                        UTextField(times, { times = it }, placeholder = stringResource(R.string.ob_times_hint), number = true, suffix = stringResource(R.string.times_suffix))
                        Spacer(Modifier.height(10.dp))
                        Segmented(Period.entries, timesPeriod, { stringResource(R.string.per, periodLabel(it)) }, { timesPeriod = it })
                    }
                    Step.MONEY -> {
                        Illustration(Icons.Rounded.Savings, U.c.gold, U.c.goldSoft)
                        Header(stringResource(R.string.ob_money_title), stringResource(R.string.ob_money_sub), center = true)
                        val symbol = runCatching { Currency.getInstance(currency).symbol }.getOrDefault(currency)
                        UTextField(money, { money = it }, placeholder = "0", number = true, suffix = symbol)
                        Spacer(Modifier.height(10.dp))
                        Segmented(Period.entries, moneyPeriod, { stringResource(R.string.per, periodLabel(it)) }, { moneyPeriod = it })
                        Text(stringResource(R.string.ob_currency_hint), style = UType.Small, modifier = Modifier.padding(top = 10.dp, start = 4.dp))
                    }
                    Step.TIME -> {
                        Illustration(Icons.Rounded.Schedule, U.c.accent, U.c.accentSoft)
                        Header(stringResource(R.string.ob_time_title), stringResource(R.string.ob_time_sub), center = true)
                        Row {
                            UTextField(hours, { hours = it }, Modifier.weight(1f), placeholder = "0", number = true, suffix = stringResource(R.string.u_hours))
                            Spacer(Modifier.width(10.dp))
                            UTextField(minutes, { minutes = it }, Modifier.weight(1f), placeholder = "0", number = true, suffix = stringResource(R.string.u_min))
                        }
                        Spacer(Modifier.height(10.dp))
                        Segmented(Period.entries, timePeriod, { stringResource(R.string.per, periodLabel(it)) }, { timePeriod = it })
                    }
                    Step.START -> {
                        Illustration(Icons.Rounded.Flag, U.c.accent, U.c.accentSoft)
                        Header(stringResource(R.string.ob_start_title), stringResource(R.string.ob_start_sub), center = true)
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                            Chip(stringResource(R.string.ob_start_now), startNow, { startNow = true })
                            Chip(stringResource(R.string.ob_start_earlier), !startNow, { startNow = false; pickDate = true })
                        }
                        if (!startNow) {
                            Spacer(Modifier.height(8.dp))
                            SoftButton(dateTimeLabel(startAt), { pickDate = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Event)
                        }
                    }
                }
            }
        }

        Column(Modifier.padding(horizontal = 22.dp, vertical = 14.dp)) {
            val label = when (step) {
                Step.WELCOME -> stringResource(R.string.ob_begin)
                Step.START -> stringResource(R.string.ob_finish)
                else -> stringResource(R.string.next)
            }
            PrimaryButton(label, next, Modifier.fillMaxWidth(), enabled = canContinue)
            if (optional) {
                TextButton(onClick = {
                    when (step) {
                        Step.TIMES -> times = ""
                        Step.MONEY -> money = ""
                        Step.TIME -> { hours = ""; minutes = "" }
                        else -> {}
                    }
                    index++
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.skip), color = U.c.muted, style = UType.Button) }
            }
        }
    }

    if (pickDate) {
        DateTimePickerDialog(startAt, onDismiss = { pickDate = false }) { startAt = it }
    }
}

@Composable
private fun Header(title: String, sub: String, top: androidx.compose.ui.unit.Dp = 24.dp, center: Boolean = false) {
    val align = if (center) TextAlign.Center else TextAlign.Start
    Text(title, style = UType.Title, textAlign = align, modifier = Modifier.fillMaxWidth().padding(top = top))
    Spacer(Modifier.height(8.dp))
    Text(sub, style = UType.Sub, textAlign = align, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(22.dp))
}

@Composable
private fun Illustration(icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color, soft: androidx.compose.ui.graphics.Color) {
    Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(150.dp).clip(CircleShape).background(soft.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
            Box(Modifier.size(104.dp).clip(CircleShape).background(soft), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(52.dp), tint = color)
            }
        }
    }
}

@Composable
private fun PresetTile(p: Preset, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .height(132.dp)
            .clip(U.RMd)
            .background(if (selected) U.c.accentSoft else U.c.surface)
            .border(1.5.dp, if (selected) U.c.accent else U.c.line, U.RMd)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        HabitIcon(p.icon, 54.dp, soft = if (selected) U.c.surface else U.c.accentSoft)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(p.label), style = UType.Strong.copy(fontSize = 15.sp, lineHeight = 18.sp), textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun Welcome() {
    Column(Modifier.fillMaxWidth().padding(top = 70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(120.dp).clip(CircleShape).background(U.c.accent), contentAlignment = Alignment.Center) {
            Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(140.dp), tint = U.c.onAccent)
        }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.app_name), style = UType.Display, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.ob_welcome_sub), style = UType.Sub.copy(fontSize = 16.sp, lineHeight = 23.sp), textAlign = TextAlign.Center)
        Spacer(Modifier.height(36.dp))
        listOf(R.string.ob_promise_1, R.string.ob_promise_2, R.string.ob_promise_3).forEach {
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(U.c.accentSoft), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = U.c.accent)
                }
                Spacer(Modifier.width(12.dp))
                Text(stringResource(it), style = UType.Body)
            }
        }
    }
}

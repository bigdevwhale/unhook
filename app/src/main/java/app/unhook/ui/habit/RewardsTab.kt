package app.unhook.ui.habit

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unhook.R
import app.unhook.container
import app.unhook.data.Reward
import app.unhook.domain.perDay
import app.unhook.ui.components.Bar
import app.unhook.ui.components.ConfirmDialog
import app.unhook.ui.components.HabitIcon
import app.unhook.ui.components.SectionTitle
import app.unhook.ui.components.SoftButton
import app.unhook.ui.components.UCard
import app.unhook.ui.count
import app.unhook.ui.minutesLabel
import app.unhook.ui.money
import app.unhook.ui.theme.U
import app.unhook.ui.theme.UType

fun LazyListScope.rewards(d: HabitData, a: HabitActions) {
    val h = d.habit
    val s = d.stats
    val hasMoney = h.money > 0
    val hasTime = h.minutes > 0
    item {
        SectionTitle(stringResource(R.string.rw_savings))
        if (!hasMoney && !hasTime && h.times <= 0) {
            UCard {
                Text(stringResource(R.string.rw_no_costs), style = UType.Sub)
                Spacer(Modifier.height(12.dp))
                SoftButton(stringResource(R.string.rw_set_costs), a.edit, Modifier.fillMaxWidth(), icon = Icons.Rounded.Tune)
            }
            return@item
        }
        UCard {
            if (hasMoney) {
                Text(stringResource(R.string.rw_balance), style = UType.Muted)
                Text(money(s.balance, d.currency), style = UType.Display.copy(fontSize = 40.sp, color = U.c.accent))
                Spacer(Modifier.height(14.dp))
                Row {
                    Stat(stringResource(R.string.rw_since_reset), money(s.savedSinceReset, d.currency), Modifier.weight(1f))
                    Stat(stringResource(R.string.rw_total), money(s.savedTotal, d.currency), Modifier.weight(1f))
                }
            }
            if (hasMoney && (hasTime || h.times > 0)) {
                HorizontalDivider(Modifier.padding(vertical = 14.dp), color = U.c.line)
            }
            Row {
                if (hasTime) Stat(stringResource(R.string.rw_time_back), minutesLabel(s.minutesRegained), Modifier.weight(1f))
                if (h.times > 0) Stat(stringResource(R.string.rw_avoided), count(s.avoidedTotal), Modifier.weight(1f))
            }
        }
    }
    if (hasMoney) {
        item {
            SectionTitle(stringResource(R.string.rw_projection))
            UCard {
                val rate = perDay(h.money, h.moneyPeriod)
                listOf(30 to R.string.rw_in_month, 365 to R.string.rw_in_year, 5 * 365 to R.string.rw_in_5years).forEach { (days, label) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Savings, null, tint = U.c.gold, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(label), style = UType.Body, modifier = Modifier.weight(1f))
                        Text(money(rate * days, d.currency), style = UType.Strong)
                    }
                }
            }
        }
        item {
            SectionTitle(stringResource(R.string.rw_wishlist))
            if (d.rewards.isEmpty()) {
                UCard {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        HabitIcon(Icons.Rounded.CardGiftcard, 64.dp, U.c.gold, U.c.goldSoft)
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.rw_wishlist_empty), style = UType.Sub, textAlign = TextAlign.Center)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        items(d.rewards, key = { it.id }) { RewardRow(it, d) }
        item {
            SoftButton(stringResource(R.string.rw_add), { a.open(Sheet.REWARD) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add, color = U.c.gold, soft = U.c.goldSoft)
        }
    } else if (hasTime) {
        item {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                Icon(Icons.Rounded.Schedule, null, tint = U.c.faint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.rw_money_hint), style = UType.Muted)
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = UType.Muted)
        Spacer(Modifier.height(2.dp))
        Text(value, style = UType.Strong.copy(fontSize = 18.sp))
    }
}

@Composable
private fun RewardRow(r: Reward, d: HabitData) {
    val context = LocalContext.current
    var confirmBuy by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val bought = r.boughtAt != null
    val fraction = if (r.price <= 0) 1f else (d.stats.balance / r.price).toFloat()
    UCard(Modifier.padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(r.title, style = UType.Strong.copy(color = if (bought) U.c.muted else U.c.ink))
                Text(money(r.price, d.currency), style = UType.Muted)
            }
            if (bought) {
                Icon(Icons.Rounded.CheckCircle, null, tint = U.c.accent)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.rw_bought), style = UType.Muted.copy(color = U.c.accent))
            } else if (fraction >= 1f) {
                TextButton(onClick = { confirmBuy = true }) { Text(stringResource(R.string.rw_buy), color = U.c.accent, style = UType.Button) }
            }
            IconButton(onClick = { confirmDelete = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.delete), tint = U.c.faint, modifier = Modifier.size(20.dp))
            }
        }
        if (!bought) {
            Spacer(Modifier.height(10.dp))
            Bar(fraction, color = U.c.gold, track = U.c.goldSoft)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("${(fraction.coerceIn(0f, 1f) * 100).toInt()}%", style = UType.Small)
                if (fraction < 1f) {
                    val rate = perDay(d.habit.money, d.habit.moneyPeriod)
                    val days = if (rate > 0) ((r.price - d.stats.balance) / rate).toInt() + 1 else null
                    if (days != null) Text(androidx.compose.ui.res.pluralStringResource(R.plurals.rw_in_days, days, days), style = UType.Small)
                }
            }
        }
    }
    if (confirmBuy) {
        ConfirmDialog(
            title = stringResource(R.string.rw_buy_title, r.title),
            text = stringResource(R.string.rw_buy_text, money(r.price, d.currency)),
            confirm = stringResource(R.string.rw_buy),
            onConfirm = { context.container.write { habits().updateReward(r.copy(boughtAt = System.currentTimeMillis())) } },
            onDismiss = { confirmBuy = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.rw_delete_title),
            text = if (bought) stringResource(R.string.rw_delete_bought) else stringResource(R.string.rw_delete_text),
            confirm = stringResource(R.string.delete),
            danger = true,
            onConfirm = { context.container.write { habits().deleteReward(r) } },
            onDismiss = { confirmDelete = false },
        )
    }
}

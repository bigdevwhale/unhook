package app.unhook.ui.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unhook.R
import app.unhook.ui.Preset
import app.unhook.ui.components.Bar
import app.unhook.ui.components.SectionTitle
import app.unhook.ui.components.SoftButton
import app.unhook.ui.components.UCard
import app.unhook.ui.quoteOfDay
import app.unhook.ui.reasonText
import app.unhook.ui.roughDuration
import app.unhook.ui.theme.U
import app.unhook.ui.theme.UType
import java.time.LocalDate

fun LazyListScope.motivation(d: HabitData, a: HabitActions) {
    item { SectionTitle(stringResource(R.string.mo_reasons)) }
    if (d.reasons.isEmpty()) {
        item { Text(stringResource(R.string.mo_reasons_empty), style = UType.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)) }
    }
    itemsIndexed(d.reasons, key = { _, r -> r.id }) { i, r ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(U.RMd)
                .background(U.c.surface)
                .clickable { a.editReason(r) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(U.c.accentSoft), contentAlignment = Alignment.Center) {
                Text((i + 1).toString(), style = UType.Small.copy(color = U.c.accent, fontWeight = FontWeight.SemiBold))
            }
            Spacer(Modifier.width(14.dp))
            Text(reasonText(r.text), style = UType.Body, modifier = Modifier.weight(1f))
        }
    }
    item {
        SoftButton(stringResource(R.string.mo_add_reason), { a.open(Sheet.REASON) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
    }

    val health = Preset.of(d.habit.kind).health
    val streakMin = d.stats.streakMs / 60_000
    item {
        SectionTitle(stringResource(R.string.mo_recovery))
        UCard {
            var prev = 0L
            health.steps.forEachIndexed { i, (minutes, text) ->
                val done = streakMin >= minutes
                val active = !done && streakMin >= prev
                val fraction = if (done) 1f else if (active) (streakMin - prev).toFloat() / (minutes - prev) else 0f
                Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(if (done) U.c.accent else if (active) U.c.accentSoft else U.c.surface2),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) Icon(Icons.Rounded.Check, null, tint = U.c.onAccent, modifier = Modifier.size(18.dp))
                        else Icon(Icons.Rounded.Favorite, null, tint = if (active) U.c.accent else U.c.line, modifier = Modifier.size(15.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(roughDuration(minutes * 60_000), style = UType.Small.copy(color = if (done || active) U.c.accent else U.c.faint))
                        Spacer(Modifier.height(2.dp))
                        Text(stringResource(text), style = UType.Body.copy(color = if (done || active) U.c.ink else U.c.muted, fontSize = 14.5.sp))
                        if (active) {
                            Spacer(Modifier.height(8.dp))
                            Bar(fraction)
                        }
                    }
                }
                prev = minutes
                if (i == health.steps.lastIndex) Spacer(Modifier.height(2.dp))
            }
            Text(stringResource(R.string.mo_recovery_note), style = UType.Small, modifier = Modifier.padding(top = 6.dp))
        }
    }
    item {
        SectionTitle(stringResource(R.string.mo_quote))
        val q = quoteOfDay(LocalDate.now(), shift = 7)
        UCard {
            Text(q.text, style = UType.Body.copy(fontSize = 16.sp, lineHeight = 24.sp))
            if (q.author.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("— " + q.author, style = UType.Muted)
            }
        }
    }
}

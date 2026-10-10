package app.unhook.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unhook.domain.DayMark
import app.unhook.domain.Milestone
import app.unhook.ui.locale
import app.unhook.ui.theme.Inter
import app.unhook.ui.theme.U
import app.unhook.ui.theme.UType
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields

private data class Tier(val main: Color, val light: Color)

@Composable
private fun tierOf(m: Milestone): Tier = when (m.ordinal) {
    0, 1, 2 -> Tier(Color(0xFFB8733A), Color(0xFFE2A774))  // bronze: days
    3, 4 -> Tier(Color(0xFF8C99A6), Color(0xFFC9D2DB))     // silver: weeks to a month
    5, 6 -> Tier(Color(0xFFD69A17), Color(0xFFF5CF66))     // gold: months
    else -> Tier(Color(0xFF16A37A), Color(0xFF7BE3BF))     // emerald: years
}

/** Shield badge; grey and hollow until earned. */
@Composable
fun TrophyBadge(m: Milestone, earned: Boolean, label: String, size: Dp = 72.dp) {
    val tier = tierOf(m)
    val lockedMain = U.c.line
    val lockedLight = U.c.surface2
    val faint = U.c.faint
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val shield = Path().apply {
                moveTo(w * 0.5f, h * 0.04f)
                cubicTo(w * 0.68f, h * 0.14f, w * 0.84f, h * 0.15f, w * 0.92f, h * 0.15f)
                lineTo(w * 0.92f, h * 0.46f)
                cubicTo(w * 0.92f, h * 0.72f, w * 0.72f, h * 0.88f, w * 0.5f, h * 0.97f)
                cubicTo(w * 0.28f, h * 0.88f, w * 0.08f, h * 0.72f, w * 0.08f, h * 0.46f)
                lineTo(w * 0.08f, h * 0.15f)
                cubicTo(w * 0.16f, h * 0.15f, w * 0.32f, h * 0.14f, w * 0.5f, h * 0.04f)
                close()
            }
            if (earned) {
                drawPath(shield, tier.main)
                // Lit left half gives the flat badge a little depth.
                val half = Path().apply {
                    moveTo(w * 0.5f, h * 0.04f)
                    cubicTo(w * 0.32f, h * 0.14f, w * 0.16f, h * 0.15f, w * 0.08f, h * 0.15f)
                    lineTo(w * 0.08f, h * 0.46f)
                    cubicTo(w * 0.08f, h * 0.72f, w * 0.28f, h * 0.88f, w * 0.5f, h * 0.97f)
                    close()
                }
                drawPath(half, tier.light.copy(alpha = 0.55f))
            } else {
                drawPath(shield, lockedLight)
                drawPath(shield, lockedMain, style = Stroke(w * 0.045f))
            }
            // Banner across the middle.
            drawRoundRect(
                if (earned) Color.White.copy(alpha = 0.92f) else lockedMain,
                topLeft = Offset(w * 0.02f, h * 0.38f),
                size = Size(w * 0.96f, h * 0.22f),
                cornerRadius = CornerRadius(w * 0.05f),
            )
            if (!earned) return@Canvas
            drawCircle(Color.White.copy(alpha = 0.85f), w * 0.045f, Offset(w * 0.5f, h * 0.76f))
        }
        Text(
            label,
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.15f).sp,
            color = if (earned) tier.main else faint,
            maxLines = 1,
        )
    }
}

/** Month grid; clean days glow green, relapses red. */
@Composable
fun MonthCalendar(mark: (LocalDate) -> DayMark, today: LocalDate, modifier: Modifier = Modifier) {
    var monthEpoch by rememberSaveable { mutableStateOf(YearMonth.from(today).let { it.year * 12 + it.monthValue - 1 }) }
    val month = YearMonth.of(monthEpoch / 12, monthEpoch % 12 + 1)
    val loc = locale()
    val first = WeekFields.of(loc).firstDayOfWeek
    val lead = Math.floorMod(month.atDay(1).dayOfWeek.value - first.value, 7)
    val cells = lead + month.lengthOfMonth()
    val rows = (cells + 6) / 7

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { monthEpoch-- }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, null, tint = U.c.muted) }
            Text(
                DateTimeFormatter.ofPattern("LLLL yyyy", loc).format(month).replaceFirstChar { it.titlecase(loc) },
                style = UType.Strong,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = { monthEpoch++ }, enabled = month < YearMonth.from(today)) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = if (month < YearMonth.from(today)) U.c.muted else U.c.line)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            repeat(7) { i ->
                Text(
                    first.plus(i.toLong()).getDisplayName(TextStyle.SHORT_STANDALONE, loc),
                    style = UType.Small,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        repeat(rows) { r ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                repeat(7) { c ->
                    val day = r * 7 + c - lead + 1
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                        if (day in 1..month.lengthOfMonth()) {
                            val d = month.atDay(day)
                            val m = mark(d)
                            val (bg, fg) = when (m) {
                                DayMark.CLEAN -> U.c.accentSoft to U.c.accent
                                DayMark.TODAY_CLEAN -> U.c.accent to U.c.onAccent
                                DayMark.RELAPSE -> U.c.dangerSoft to U.c.danger
                                DayMark.NONE -> Color.Transparent to if (d.isAfter(today)) U.c.faint else U.c.muted
                            }
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(bg)
                                    .then(if (d == today && m != DayMark.TODAY_CLEAN) Modifier.border(1.5.dp, U.c.danger, CircleShape) else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    day.toString(),
                                    fontFamily = Inter,
                                    fontWeight = if (m == DayMark.NONE) FontWeight.Normal else FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = fg,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Plain bar chart of streak lengths, current streak highlighted at the end. */
@Composable
fun StreakChart(values: List<Long>, modifier: Modifier = Modifier) {
    val shown = values.takeLast(12)
    val max = (shown.maxOrNull() ?: 1L).coerceAtLeast(1L)
    val accent = U.c.accent
    val soft = U.c.accentSoft
    val muted = U.c.faint.copy(alpha = 0.45f)
    Canvas(modifier.fillMaxWidth().height(140.dp)) {
        if (shown.isEmpty()) return@Canvas
        val gap = 8.dp.toPx()
        val bw = ((size.width - gap * (shown.size - 1)) / shown.size).coerceAtMost(46.dp.toPx())
        val total = bw * shown.size + gap * (shown.size - 1)
        var x = (size.width - total) / 2
        shown.forEachIndexed { i, v ->
            val h = (size.height * v / max).coerceAtLeast(4.dp.toPx())
            val last = i == shown.lastIndex
            drawRoundRect(soft, Offset(x, 0f), Size(bw, size.height), CornerRadius(8.dp.toPx()))
            drawRoundRect(if (last) accent else muted, Offset(x, size.height - h), Size(bw, h), CornerRadius(8.dp.toPx()))
            x += bw + gap
        }
    }
}

package app.unhook.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import app.unhook.R
import app.unhook.data.Period
import app.unhook.domain.DAY_MS
import app.unhook.domain.Milestone
import app.unhook.domain.partsOf
import app.unhook.notify.Milestones
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun locale(): Locale = LocalConfiguration.current.locales[0]

@Composable
fun milestoneLabel(m: Milestone): String {
    LocalConfiguration.current // recompose on language change
    return Milestones.milestoneText(LocalContext.current, m)
}

@Composable
fun periodLabel(p: Period): String = stringResource(
    when (p) {
        Period.DAY -> R.string.period_day
        Period.WEEK -> R.string.period_week
        Period.MONTH -> R.string.period_month
    }
)

/** "3d 4h 12m", or "4h 12m 5s" under a day: compact, for cards and stats. */
@Composable
fun shortDuration(ms: Long, seconds: Boolean = false): String {
    val p = partsOf(ms)
    val d = stringResource(R.string.u_d)
    val h = stringResource(R.string.u_h)
    val m = stringResource(R.string.u_m)
    val s = stringResource(R.string.u_s)
    return when {
        p.days > 0 -> "${p.days}$d ${p.hours}$h ${p.minutes}$m"
        p.hours > 0 -> "${p.hours}$h ${p.minutes}$m" + if (seconds) " ${p.seconds}$s" else ""
        else -> "${p.minutes}$m" + if (seconds) " ${p.seconds}$s" else ""
    }
}

/** "2 years", "3 months", "12 days", "5 hours": the single largest unit. */
@Composable
fun roughDuration(ms: Long): String {
    val minutes = (ms / 60_000).toInt()
    val days = (ms / DAY_MS).toInt()
    return when {
        days >= 365 -> (days / 365).let { pluralStringResource(R.plurals.years, it, it) }
        days >= 60 -> (days / 30).let { pluralStringResource(R.plurals.months, it, it) }
        days >= 1 -> pluralStringResource(R.plurals.days, days, days)
        minutes >= 60 -> (minutes / 60).let { pluralStringResource(R.plurals.hours, it, it) }
        else -> pluralStringResource(R.plurals.minutes, minutes, minutes)
    }
}

/** Regained time: "3 h 20 min" or "2 days 4 h". */
@Composable
fun minutesLabel(total: Double): String {
    val m = total.roundToLong()
    val days = m / (24 * 60)
    val hours = m % (24 * 60) / 60
    val mins = m % 60
    val h = stringResource(R.string.u_hours)
    val mn = stringResource(R.string.u_min)
    return when {
        days > 0 -> pluralStringResource(R.plurals.days, days.toInt(), days.toInt()) + " $hours $h"
        hours > 0 -> "$hours $h $mins $mn"
        else -> "$mins $mn"
    }
}

@Composable
fun money(amount: Double, currency: String): String {
    val f = NumberFormat.getCurrencyInstance(locale())
    runCatching { f.currency = Currency.getInstance(currency) }
    f.maximumFractionDigits = if (amount >= 1000) 0 else 2
    f.minimumFractionDigits = f.maximumFractionDigits
    return f.format(amount)
}

@Composable
fun count(amount: Double): String {
    val f = NumberFormat.getNumberInstance(locale())
    f.maximumFractionDigits = if (amount >= 10) 0 else 1
    return f.format(amount)
}

@Composable
fun dateLabel(ms: Long): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale())
        .format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

@Composable
fun dateTimeLabel(ms: Long): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale())
        .format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

@Composable
fun shortDateLabel(d: LocalDate): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale()).format(d)

data class Quote(val text: String, val author: String)

/** Same quote all day, a new one tomorrow. */
@Composable
fun quoteOfDay(today: LocalDate, shift: Int = 0): Quote {
    val all = stringArrayResource(R.array.quotes)
    val q = all[Math.floorMod(today.toEpochDay().toInt() + shift, all.size)]
    return Quote(q.substringBefore('|').trim(), q.substringAfter('|', "").trim())
}

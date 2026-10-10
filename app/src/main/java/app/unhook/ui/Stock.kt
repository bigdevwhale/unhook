package app.unhook.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Habit names and reasons are saved as text in the language of the moment. When that text is
 * one of our stock presets or suggestions (in any app language), show it in the current one.
 * The user's own wording is left as typed.
 */
object Stock {
    private val languages = listOf("en", "ru")

    private class Index(val labels: Map<String, Int>, val reasons: Map<String, Pair<Int, Int>>)

    @Volatile
    private var index: Index? = null

    private fun index(context: Context): Index = index ?: synchronized(this) {
        index ?: run {
            val labels = HashMap<String, Int>()
            val reasons = HashMap<String, Pair<Int, Int>>()
            for (lang in languages) {
                val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(lang)) }
                val res = context.createConfigurationContext(config).resources
                for (p in Preset.entries) {
                    if (p != Preset.CUSTOM) labels[res.getString(p.label)] = p.label
                    res.getStringArray(p.reasons).forEachIndexed { i, s -> reasons[s] = p.reasons to i }
                }
            }
            Index(labels, reasons).also { index = it }
        }
    }

    fun name(context: Context, raw: String): String =
        index(context).labels[raw]?.let { context.getString(it) } ?: raw

    fun reason(context: Context, raw: String): String =
        index(context).reasons[raw]?.let { (array, i) -> context.resources.getStringArray(array).getOrNull(i) } ?: raw
}

@Composable
fun habitName(raw: String): String {
    LocalConfiguration.current // recompose on language change
    return Stock.name(LocalContext.current, raw)
}

@Composable
fun reasonText(raw: String): String {
    LocalConfiguration.current
    return Stock.reason(LocalContext.current, raw)
}

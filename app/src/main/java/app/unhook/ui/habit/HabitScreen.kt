package app.unhook.ui.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.unhook.R
import app.unhook.container
import app.unhook.data.Entry
import app.unhook.data.Habit
import app.unhook.data.Reason
import app.unhook.data.Relapse
import app.unhook.data.Reward
import app.unhook.domain.Stats
import app.unhook.domain.statsOf
import app.unhook.ui.components.TopBar
import app.unhook.ui.components.rememberNow
import app.unhook.ui.habitName
import app.unhook.ui.theme.U
import app.unhook.ui.theme.UType

/** Everything a tab needs about one habit, recomputed every second for the live counters. */
class HabitData(
    val habit: Habit,
    val relapses: List<Relapse>,
    val reasons: List<Reason>,
    val rewards: List<Reward>,
    val entries: List<Entry>,
    val stats: Stats,
    val now: Long,
    val currency: String,
)

private enum class Tab(val icon: ImageVector, val label: Int) {
    OVERVIEW(Icons.Rounded.Today, R.string.tab_overview),
    REWARDS(Icons.Rounded.CardGiftcard, R.string.tab_rewards),
    MOTIVATION(Icons.Rounded.Lightbulb, R.string.tab_motivation),
    STATS(Icons.Rounded.Insights, R.string.tab_stats),
    TROPHIES(Icons.Rounded.EmojiEvents, R.string.tab_trophies),
}

@Composable
fun HabitScreen(id: Long, currency: String, onBack: () -> Unit, onEdit: () -> Unit) {
    val context = LocalContext.current
    val dao = context.container.db.habits()
    val habitFlow = remember(id) { dao.observe(id) }
    val habit by habitFlow.collectAsStateWithLifecycle(initialValue = null)
    val relapses by remember(id) { dao.observeRelapses(id) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val reasons by remember(id) { dao.observeReasons(id) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val rewards by remember(id) { dao.observeRewards(id) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val entries by remember(id) { dao.observeEntries(id) }.collectAsStateWithLifecycle(initialValue = emptyList())
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val now = rememberNow()

    var sheet by rememberSaveable { mutableStateOf<Sheet?>(null) }
    var editingReason by rememberSaveable { mutableLongStateOf(0L) }
    // Null while loading, and for a moment after a delete before navigation pops this screen.
    val h = habit ?: return

    val data = HabitData(h, relapses, reasons, rewards, entries, statsOf(h, relapses, rewards, now), now, currency)
    val actions = HabitActions(
        open = { sheet = it },
        editReason = { editingReason = it.id; sheet = Sheet.REASON },
        edit = onEdit,
    )

    Column(Modifier.fillMaxSize().background(U.c.bg)) {
        Box(Modifier.statusBarsPadding()) {
            TopBar(habitName(h.name), onBack) {
                IconButton(onClick = onEdit) { Icon(Icons.Rounded.Tune, stringResource(R.string.edit), tint = U.c.muted) }
            }
        }
        // Fresh list state per tab, so switching tabs always starts at the top.
        val listState = androidx.compose.runtime.key(tab) { androidx.compose.foundation.lazy.rememberLazyListState() }
        LazyColumn(
            Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
        ) {
            when (Tab.entries[tab]) {
                Tab.OVERVIEW -> overview(data, actions)
                Tab.REWARDS -> rewards(data, actions)
                Tab.MOTIVATION -> motivation(data, actions)
                Tab.STATS -> stats(data)
                Tab.TROPHIES -> trophies(data)
            }
        }
        HabitSheets(data, sheet, reasons.firstOrNull { it.id == editingReason }, onClose = {
            sheet = null
            editingReason = 0L
        }, onSwitch = { sheet = it })
        NavigationBar(containerColor = U.c.surface, tonalElevation = 0.dp) {
            Tab.entries.forEachIndexed { i, t ->
                NavigationBarItem(
                    selected = tab == i,
                    onClick = { tab = i },
                    icon = { Icon(t.icon, null) },
                    label = { Text(stringResource(t.label), style = UType.Small.copy(color = if (tab == i) U.c.accent else U.c.muted), maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = U.c.accent,
                        unselectedIconColor = U.c.muted,
                        indicatorColor = U.c.accentSoft,
                    ),
                )
            }
        }
    }
}

enum class Sheet { CRAVING, RELAPSE, NOTE, REWARD, REASON }

/** What tab content can ask the screen to do; sheets live here so scrolling never disposes them. */
class HabitActions(
    val open: (Sheet) -> Unit,
    val editReason: (Reason) -> Unit,
    val edit: () -> Unit,
)

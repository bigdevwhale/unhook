package app.unhook.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.unhook.ui.HabitIcons
import app.unhook.ui.theme.U

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPicker(selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HabitIcons.all.forEach { (key, icon) ->
            val on = key == selected
            Box(
                Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(if (on) U.c.accent else U.c.surface)
                    .border(1.dp, if (on) U.c.accent else U.c.line, CircleShape)
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center,
            ) { Icon(icon, key, tint = if (on) U.c.onAccent else U.c.muted) }
        }
    }
}

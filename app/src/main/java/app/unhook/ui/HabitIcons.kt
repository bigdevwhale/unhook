package app.unhook.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Icecream
import androidx.compose.material.icons.rounded.LocalPizza
import androidx.compose.material.icons.rounded.Liquor
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Nightlife
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SportsBar
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector
import app.unhook.data.Habit

/** Every icon a habit can wear: the presets' own first, then extras for custom habits. */
object HabitIcons {
    private val extras = listOf(
        "tv" to Icons.Rounded.Tv,
        "movie" to Icons.Rounded.Movie,
        "bedtime" to Icons.Rounded.Bedtime,
        "pizza" to Icons.Rounded.LocalPizza,
        "icecream" to Icons.Rounded.Icecream,
        "restaurant" to Icons.Rounded.Restaurant,
        "beer" to Icons.Rounded.SportsBar,
        "liquor" to Icons.Rounded.Liquor,
        "nightlife" to Icons.Rounded.Nightlife,
        "pills" to Icons.Rounded.Medication,
        "chat" to Icons.Rounded.Chat,
        "money" to Icons.Rounded.AttachMoney,
        "music" to Icons.Rounded.Headphones,
        "car" to Icons.Rounded.DirectionsCar,
        "anger" to Icons.Rounded.Whatshot,
        "mind" to Icons.Rounded.Psychology,
        "mood" to Icons.Rounded.Mood,
        "brush" to Icons.Rounded.Brush,
        "heart" to Icons.Rounded.Favorite,
    )

    val all: List<Pair<String, ImageVector>> = Preset.entries.map { it.key to it.icon } + extras

    private val byKey = all.toMap()

    fun of(key: String): ImageVector? = byKey[key]
}

/** The icon the user picked, or the preset's own. */
val Habit.iconVector: ImageVector get() = HabitIcons.of(icon) ?: Preset.of(kind).icon

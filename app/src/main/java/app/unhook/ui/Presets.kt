package app.unhook.ui

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BackHand
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.NoAdultContent
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SmokingRooms
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VapingRooms
import androidx.compose.ui.graphics.vector.ImageVector
import app.unhook.R

/** Habits offered in onboarding, with suggested reasons and (where known) a recovery timeline. */
enum class Preset(
    val key: String,
    val icon: ImageVector,
    @StringRes val label: Int,
    @ArrayRes val reasons: Int,
    val health: Health,
) {
    SMOKING("smoking", Icons.Rounded.SmokingRooms, R.string.preset_smoking, R.array.reasons_smoking, Health.SMOKING),
    ALCOHOL("alcohol", Icons.Rounded.LocalBar, R.string.preset_alcohol, R.array.reasons_alcohol, Health.ALCOHOL),
    PORN("porn", Icons.Rounded.NoAdultContent, R.string.preset_porn, R.array.reasons_porn, Health.GENERIC),
    SUGAR("sugar", Icons.Rounded.Cake, R.string.preset_sugar, R.array.reasons_sugar, Health.SUGAR),
    GAMBLING("gambling", Icons.Rounded.Casino, R.string.preset_gambling, R.array.reasons_gambling, Health.GENERIC),
    PROCRASTINATION("procrastination", Icons.Rounded.HourglassBottom, R.string.preset_procrastination, R.array.reasons_procrastination, Health.GENERIC),
    VAPING("vaping", Icons.Rounded.VapingRooms, R.string.preset_vaping, R.array.reasons_smoking, Health.SMOKING),
    SOCIAL("social_media", Icons.Rounded.PhoneIphone, R.string.preset_social, R.array.reasons_social, Health.GENERIC),
    GAMING("gaming", Icons.Rounded.SportsEsports, R.string.preset_gaming, R.array.reasons_generic, Health.GENERIC),
    FAST_FOOD("fast_food", Icons.Rounded.Fastfood, R.string.preset_fast_food, R.array.reasons_sugar, Health.SUGAR),
    COFFEE("coffee", Icons.Rounded.Coffee, R.string.preset_coffee, R.array.reasons_generic, Health.GENERIC),
    ENERGY("energy_drinks", Icons.Rounded.Bolt, R.string.preset_energy, R.array.reasons_generic, Health.GENERIC),
    CANNABIS("cannabis", Icons.Rounded.Grass, R.string.preset_cannabis, R.array.reasons_generic, Health.GENERIC),
    SHOPPING("shopping", Icons.Rounded.ShoppingBag, R.string.preset_shopping, R.array.reasons_gambling, Health.GENERIC),
    NAILS("nail_biting", Icons.Rounded.BackHand, R.string.preset_nails, R.array.reasons_generic, Health.GENERIC),
    CUSTOM("custom", Icons.Rounded.Star, R.string.preset_custom, R.array.reasons_generic, Health.GENERIC);

    companion object {
        /** The six big ones shown first; the rest sit behind "More habits". */
        val featured = listOf(SMOKING, ALCOHOL, PORN, SUGAR, GAMBLING, PROCRASTINATION)

        fun of(key: String): Preset = entries.firstOrNull { it.key == key } ?: CUSTOM
    }
}

/** What the body and mind regain, by elapsed clean time in minutes. */
enum class Health(val steps: List<Pair<Long, Int>>) {
    SMOKING(
        listOf(
            20L to R.string.h_smoke_20m,
            8 * 60L to R.string.h_smoke_8h,
            24 * 60L to R.string.h_smoke_1d,
            2 * 24 * 60L to R.string.h_smoke_2d,
            3 * 24 * 60L to R.string.h_smoke_3d,
            14 * 24 * 60L to R.string.h_smoke_2w,
            30 * 24 * 60L to R.string.h_smoke_1m,
            90 * 24 * 60L to R.string.h_smoke_3m,
            365 * 24 * 60L to R.string.h_smoke_1y,
            5 * 365 * 24 * 60L to R.string.h_smoke_5y,
            10 * 365 * 24 * 60L to R.string.h_smoke_10y,
        )
    ),
    ALCOHOL(
        listOf(
            24 * 60L to R.string.h_alc_1d,
            3 * 24 * 60L to R.string.h_alc_3d,
            7 * 24 * 60L to R.string.h_alc_1w,
            14 * 24 * 60L to R.string.h_alc_2w,
            30 * 24 * 60L to R.string.h_alc_1m,
            90 * 24 * 60L to R.string.h_alc_3m,
            365 * 24 * 60L to R.string.h_alc_1y,
        )
    ),
    SUGAR(
        listOf(
            24 * 60L to R.string.h_sugar_1d,
            3 * 24 * 60L to R.string.h_sugar_3d,
            7 * 24 * 60L to R.string.h_sugar_1w,
            14 * 24 * 60L to R.string.h_sugar_2w,
            30 * 24 * 60L to R.string.h_sugar_1m,
            90 * 24 * 60L to R.string.h_sugar_3m,
        )
    ),
    GENERIC(
        listOf(
            24 * 60L to R.string.h_gen_1d,
            3 * 24 * 60L to R.string.h_gen_3d,
            7 * 24 * 60L to R.string.h_gen_1w,
            30 * 24 * 60L to R.string.h_gen_1m,
            90 * 24 * 60L to R.string.h_gen_3m,
            365 * 24 * 60L to R.string.h_gen_1y,
        )
    ),
}

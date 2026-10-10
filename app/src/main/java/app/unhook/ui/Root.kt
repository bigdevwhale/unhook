package app.unhook.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.unhook.container
import app.unhook.data.Habit
import app.unhook.data.Settings
import app.unhook.notify.Milestones
import app.unhook.ui.habit.EditHabitScreen
import app.unhook.ui.habit.HabitScreen
import app.unhook.ui.home.HomeScreen
import app.unhook.ui.onboarding.OnboardingScreen
import app.unhook.ui.settings.SettingsScreen
import app.unhook.ui.theme.U
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

@Composable
fun UnhookRoot(openHabit: MutableStateFlow<Long?>, settings: Settings) {
    val context = LocalContext.current
    // null until the database answers, so the first frame is neither home nor onboarding by mistake.
    val hasHabits by remember { context.container.db.habits().observeAll().map(List<Habit>::isNotEmpty) }
        .collectAsStateWithLifecycle(initialValue = null)
    val ready = hasHabits ?: run {
        Box(Modifier.fillMaxSize().background(U.c.bg))
        return
    }

    val nav = rememberNavController()
    val start = remember { if (ready) "home" else "welcome" }

    val requested by openHabit.collectAsStateWithLifecycle()
    LaunchedEffect(requested) {
        val id = requested ?: return@LaunchedEffect
        if (ready && id > 0) nav.navigate("habit/$id") { launchSingleTop = true }
        openHabit.value = null
    }

    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(ready) {
        // Ask once there is something to celebrate, not on the welcome screen.
        if (ready && Build.VERSION.SDK_INT >= 33 && !Milestones.canNotify(context)) {
            askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    NavHost(navController = nav, startDestination = start) {
        composable("welcome") {
            OnboardingScreen(first = true, currency = settings.currency, onCancel = null) { id ->
                nav.navigate("home") { popUpTo("welcome") { inclusive = true } }
                nav.navigate("habit/$id")
            }
        }
        composable("add") {
            OnboardingScreen(first = false, currency = settings.currency, onCancel = { nav.popBackStack() }) { id ->
                nav.navigate("habit/$id") { popUpTo("home") }
            }
        }
        composable("home") {
            HomeScreen(
                onOpen = { nav.navigate("habit/$it") },
                onAdd = { nav.navigate("add") },
                onSettings = { nav.navigate("settings") },
            )
        }
        composable("habit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            HabitScreen(
                id = id,
                currency = settings.currency,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate("edit/$id") },
            )
        }
        composable("edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            EditHabitScreen(
                id = entry.arguments?.getLong("id") ?: 0L,
                currency = settings.currency,
                onDone = { nav.popBackStack() },
                onDeleted = { nav.popBackStack("home", inclusive = false) },
            )
        }
        composable("settings") {
            SettingsScreen(settings = settings, onBack = { nav.popBackStack() })
        }
    }
}

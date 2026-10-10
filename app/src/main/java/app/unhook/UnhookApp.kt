package app.unhook

import android.app.Application
import android.content.Context
import app.unhook.data.DemoSeed
import app.unhook.data.SettingsStore
import app.unhook.data.UnhookDatabase
import app.unhook.notify.Milestones
import app.unhook.widget.CounterWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(private val context: Context) {
    val db = UnhookDatabase.create(context)
    val settings = SettingsStore(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Fire-and-forget so a write survives the screen being closed right after tapping save. */
    fun write(block: suspend UnhookDatabase.() -> Unit) {
        scope.launch {
            db.block()
            changed()
        }
    }

    /** Anything that moves a timer: re-arm milestone alarms and refresh home-screen widgets. */
    suspend fun changed() {
        Milestones.reschedule(context)
        CounterWidget.refresh(context)
    }
}

class UnhookApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Milestones.ensureChannel(this)
        // Re-arm on every start: OEM task killers (HyperOS & co.) silently drop alarms.
        container.scope.launch {
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(this@UnhookApp, container.db, container.settings)
            container.settings.pinCurrency()
            container.changed()
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as UnhookApp).container

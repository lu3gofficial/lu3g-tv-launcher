package it.lu3g.tvlauncher.dream

import android.service.dreams.DreamService
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import it.lu3g.tvlauncher.data.Prefs
import it.lu3g.tvlauncher.theme.Seasons
import it.lu3g.tvlauncher.ui.theme.Lu3gTheme

/**
 * Salvaschermo LU3G ("Daydream" di Android): parte dopo l'inattività al posto di quello di Google.
 * Un DreamService non ha un ciclo di vita Compose: lo forniamo noi, così le animazioni
 * (sfondo, decorazioni) si fermano quando il salvaschermo finisce. Qualsiasi tasto lo chiude.
 *
 * Per impostarlo: Impostazioni → Sistema → Salvaschermo, oppure via ADB:
 *   settings put secure screensaver_components it.lu3g.tvlauncher/.dream.Lu3gDreamService
 */
class Lu3gDreamService : DreamService(), LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = true
        val prefs = Prefs(this)
        val season = Seasons.resolve(prefs.seasonalEnabled.value, prefs.seasonChoice.value)
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@Lu3gDreamService)
            setViewTreeSavedStateRegistryOwner(this@Lu3gDreamService)
            setContent { Lu3gTheme { DreamScreen(season) } }
        }
        setContentView(view)
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onDreamingStopped() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDetachedFromWindow()
    }
}

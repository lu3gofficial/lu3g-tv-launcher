package it.lu3g.tvlauncher

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.lu3g.tvlauncher.data.AppEntry
import it.lu3g.tvlauncher.data.AppRepository
import it.lu3g.tvlauncher.data.Prefs
import it.lu3g.tvlauncher.data.TvInput
import it.lu3g.tvlauncher.theme.SeasonId
import it.lu3g.tvlauncher.theme.SeasonTheme
import it.lu3g.tvlauncher.theme.Seasons
import it.lu3g.tvlauncher.update.UpdateChecker
import it.lu3g.tvlauncher.update.UpdateInfo
import it.lu3g.tvlauncher.update.UpdateInstaller
import it.lu3g.tvlauncher.update.UpdateState
import it.lu3g.tvlauncher.voice.BREAK_INTERVALS
import it.lu3g.tvlauncher.voice.BreakScheduler
import it.lu3g.tvlauncher.voice.QuietHours
import it.lu3g.tvlauncher.voice.VOICE_VOLUMES
import it.lu3g.tvlauncher.voice.VoiceAssistant
import it.lu3g.tvlauncher.voice.VoiceCategory
import it.lu3g.tvlauncher.voice.VoiceSettings
import it.lu3g.tvlauncher.voice.VoiceTone
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class UiState(
    val loading: Boolean = true,
    val apps: List<AppEntry> = emptyList(),
    val favorites: List<AppEntry> = emptyList(),
    val recents: List<AppEntry> = emptyList(),
    val games: List<AppEntry> = emptyList(),
    val inputs: List<TvInput> = emptyList(),
    val focused: AppEntry? = null,
    val menuFor: AppEntry? = null,
    val menuIsFavorite: Boolean = false,
    /** Posizione dell'app del menu tra i preferiti (-1 se non lo è). */
    val menuFavoriteIndex: Int = -1,
    val lastLaunch: Map<String, Long> = emptyMap(),
)

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppRepository(app)
    private val prefs = Prefs(app)

    private val apps = MutableStateFlow<List<AppEntry>>(emptyList())
    private val inputs = MutableStateFlow<List<TvInput>>(emptyList())
    private val loading = MutableStateFlow(true)
    private val focused = MutableStateFlow<AppEntry?>(null)
    private val menuFor = MutableStateFlow<AppEntry?>(null)

    // --- Tema stagionale ---
    private val dayTick = MutableStateFlow(0)
    val seasonalEnabled: StateFlow<Boolean> = prefs.seasonalEnabled
    val seasonChoice: StateFlow<SeasonId> = prefs.seasonChoice
    /** Tema stagionale in vigore (null = solo brand). Rivalutato a ogni refresh e ogni ora. */
    val seasonTheme: StateFlow<SeasonTheme?> =
        combine(prefs.seasonalEnabled, prefs.seasonChoice, dayTick) { enabled, choice, _ -> Seasons.resolve(enabled, choice) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, Seasons.resolve(prefs.seasonalEnabled.value, prefs.seasonChoice.value))

    private val _themeSettingsOpen = MutableStateFlow(false)
    val themeSettingsOpen: StateFlow<Boolean> = _themeSettingsOpen

    fun openThemeSettings() { _themeSettingsOpen.value = true }
    fun closeThemeSettings() { _themeSettingsOpen.value = false }
    fun setSeasonalEnabled(enabled: Boolean) = prefs.setSeasonalEnabled(enabled)
    fun chooseSeason(id: SeasonId) = prefs.setSeasonChoice(id)

    // --- Aggiornamenti da GitHub Releases ---
    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState
    private val _updateDialogOpen = MutableStateFlow(false)
    val updateDialogOpen: StateFlow<Boolean> = _updateDialogOpen
    /** Esito dell'ultimo controllo, per il pannello Tema (null = mai controllato). */
    private val _updateHint = MutableStateFlow<String?>(null)
    val updateHint: StateFlow<String?> = _updateHint

    fun checkForUpdates(manual: Boolean = false) {
        if (_updateState.value == UpdateState.Checking) return
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            if (manual) _updateHint.value = getApplication<Application>().getString(R.string.update_checking)
            val result = runCatching { UpdateChecker.check(BuildConfig.VERSION_NAME) }
            val app = getApplication<Application>()
            result.onSuccess { info ->
                _updateInfo.value = info
                _updateHint.value = if (info != null) app.getString(R.string.update_found, info.version) else app.getString(R.string.update_none)
                if (manual && info != null) _updateDialogOpen.value = true
            }.onFailure {
                android.util.Log.w("Lu3gUpdate", "controllo non riuscito", it)
                _updateHint.value = app.getString(R.string.update_failed_check)
            }
            _updateState.value = UpdateState.Idle
        }
    }

    fun openUpdateDialog() { if (_updateInfo.value != null) _updateDialogOpen.value = true }
    fun closeUpdateDialog() { _updateDialogOpen.value = false }

    fun startUpdate() {
        val info = _updateInfo.value ?: return
        val app = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !app.packageManager.canRequestPackageInstalls()) {
            _updateState.value = UpdateState.NeedsPermission
            val settings = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))
            start(if (app.packageManager.resolveActivity(settings, 0) != null) settings else Intent(Settings.ACTION_SECURITY_SETTINGS))
            return
        }
        viewModelScope.launch {
            _updateState.value = UpdateState.Downloading(0f)
            runCatching {
                UpdateInstaller.downloadAndInstall(app, info) { p -> _updateState.value = UpdateState.Downloading(p) }
            }.onSuccess {
                _updateState.value = UpdateState.Installing
            }.onFailure {
                android.util.Log.w("Lu3gUpdate", "download/installazione non riusciti", it)
                _updateState.value = UpdateState.Error(it.message ?: "errore")
            }
        }
    }

    // --- Assistente vocale: pausa caffè e altre categorie ---
    private val assistant = VoiceAssistant(app)
    private val scheduler = BreakScheduler()
    private val foreground = MutableStateFlow(false)
    val voiceSettings: StateFlow<VoiceSettings> = prefs.voiceSettings
    private val _breakPrompt = MutableStateFlow<Pair<VoiceCategory, String>?>(null)
    /** Pausa proposta in questo momento (categoria e frase), o null. */
    val breakPrompt: StateFlow<Pair<VoiceCategory, String>?> = _breakPrompt
    private val _voiceSettingsOpen = MutableStateFlow(false)
    val voiceSettingsOpen: StateFlow<Boolean> = _voiceSettingsOpen

    /** Voci italiane disponibili (nome tecnico, etichetta leggibile). */
    fun italianVoices(): List<Pair<String, String>> = assistant.italianVoices().mapIndexed { i, v ->
        v.name to ("Voce ${i + 1}" + if (v.isNetworkConnectionRequired) " · online" else "")
    }
    fun ttsAvailable(): Boolean = assistant.italianVoices().isNotEmpty()

    fun setForeground(value: Boolean) { foreground.value = value }
    fun openVoiceSettings() { _voiceSettingsOpen.value = true }
    fun closeVoiceSettings() { _voiceSettingsOpen.value = false }

    fun setVoiceEnabled(enabled: Boolean) = prefs.updateVoice { it.copy(enabled = enabled) }
    fun cycleInterval() = prefs.updateVoice { s -> s.copy(intervalMin = BREAK_INTERVALS[(BREAK_INTERVALS.indexOf(s.intervalMin) + 1).mod(BREAK_INTERVALS.size)]) }
    fun cycleQuiet() = prefs.updateVoice { s -> s.copy(quiet = QuietHours.entries[(s.quiet.ordinal + 1) % QuietHours.entries.size]) }
    fun cycleTone() = prefs.updateVoice { s -> s.copy(tone = VoiceTone.entries[(s.tone.ordinal + 1) % VoiceTone.entries.size]) }
    fun cycleVolume() = prefs.updateVoice { s -> s.copy(volume = VOICE_VOLUMES[(VOICE_VOLUMES.indexOfFirst { it >= s.volume - 0.01f } + 1).mod(VOICE_VOLUMES.size)]) }
    fun cycleVoice() = prefs.updateVoice { s ->
        val names = italianVoices().map { it.first }
        val next = if (names.isEmpty()) null else names.getOrNull(names.indexOf(s.voiceName) + 1) // oltre l'ultima: predefinita
        s.copy(voiceName = next)
    }
    fun toggleCategory(c: VoiceCategory) = prefs.updateVoice { s -> s.copy(categories = if (c.id in s.categories) s.categories - c.id else s.categories + c.id) }

    fun previewVoice() = speak(VoiceCategory.COFFEE.randomPhrase())
    fun showBreakNow() = showBreak(VoiceCategory.COFFEE)

    private fun showBreak(category: VoiceCategory) {
        val phrase = category.randomPhrase()
        scheduler.onShown(System.currentTimeMillis(), category)
        _breakPrompt.value = category to phrase
        speak(phrase)
        viewModelScope.launch {
            delay(25_000)
            if (_breakPrompt.value?.second == phrase) ignoreBreak()
        }
    }

    fun confirmBreak() {
        val current = _breakPrompt.value ?: return
        scheduler.onConfirm(System.currentTimeMillis())
        _breakPrompt.value = null
        speak(current.first.confirm)
    }

    fun snoozeBreak() {
        scheduler.onSnooze(System.currentTimeMillis())
        _breakPrompt.value = null
        speak(VoiceCategory.SNOOZE_PHRASE)
    }

    fun ignoreBreak() {
        if (_breakPrompt.value == null) return
        scheduler.onIgnore(System.currentTimeMillis())
        _breakPrompt.value = null
        assistant.stop()
    }

    private fun speak(text: String) {
        val s = voiceSettings.value
        if (s.enabled) assistant.speak(text, s.voiceName, s.tone, s.volume)
    }

    override fun onCleared() {
        assistant.shutdown()
        super.onCleared()
    }

    private val _showSplash = MutableStateFlow(true)
    /** Splash visibile: al primo avvio e a ogni risveglio della TV dallo standby. */
    val showSplash: StateFlow<Boolean> = _showSplash

    private val _splashRunning = MutableStateFlow(true)
    /** Animazione e suono in corso. Se la splash è visibile ma non in corso, mostra solo nero. */
    val splashRunning: StateFlow<Boolean> = _splashRunning

    private val _homeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emesso quando l'utente preme Home mentre il launcher è già aperto. */
    val homeEvents: SharedFlow<Unit> = _homeEvents

    private data class Catalog(
        val all: List<AppEntry>,
        val favorites: List<AppEntry>,
        val recents: List<AppEntry>,
        val lastLaunch: Map<String, Long>,
    )

    private val catalog = combine(apps, prefs.favorites, prefs.recents, prefs.lastLaunch) { list, favs, recents, last ->
        val byPkg = list.associateBy { it.packageName }
        Catalog(
            all = list,
            favorites = favs.mapNotNull { byPkg[it] },
            recents = recents.mapNotNull { byPkg[it] }.filter { it.packageName !in favs }.take(8),
            lastLaunch = last,
        )
    }

    val uiState: StateFlow<UiState> =
        combine(catalog, inputs, loading, focused, menuFor) { cat, inputs, loading, focused, menu ->
            UiState(
                loading = loading,
                apps = cat.all,
                favorites = cat.favorites,
                recents = cat.recents,
                games = cat.all.filter { it.isGame },
                inputs = inputs,
                focused = focused,
                menuFor = menu,
                menuIsFavorite = menu != null && cat.favorites.any { it.packageName == menu.packageName },
                menuFavoriteIndex = menu?.let { m -> cat.favorites.indexOfFirst { it.packageName == m.packageName } } ?: -1,
                lastLaunch = cat.lastLaunch,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    init {
        refresh()
        // Pianificatore delle pause: controllo ogni 30 secondi, mai sopra altre finestre
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                val s = voiceSettings.value
                val quiet = _breakPrompt.value != null || _voiceSettingsOpen.value || _themeSettingsOpen.value ||
                    menuFor.value != null || _showSplash.value
                if (!s.enabled || quiet) continue
                scheduler.due(System.currentTimeMillis(), foreground.value, s.intervalMin, s.quiet, s.categories)
                    ?.let { showBreak(it) }
            }
        }
        // Controllo aggiornamenti: poco dopo l'avvio, poi ogni 12 ore
        viewModelScope.launch {
            delay(15_000)
            while (true) {
                checkForUpdates()
                delay(12 * 60 * 60 * 1000L)
            }
        }
        // Cambio di giorno (e quindi di tema) anche se il launcher resta aperto a lungo
        viewModelScope.launch {
            while (true) {
                delay(60 * 60 * 1000L)
                dayTick.value++
            }
        }
    }

    fun refresh() {
        dayTick.value++
        viewModelScope.launch {
            val list = repo.loadApps()
            apps.value = list
            inputs.value = repo.loadInputs()
            loading.value = false
            val current = focused.value
            if (current == null || list.none { it.packageName == current.packageName }) focused.value = list.firstOrNull()
            menuFor.value?.let { m -> if (list.none { it.packageName == m.packageName }) menuFor.value = null }
        }
    }

    fun onFocus(app: AppEntry) {
        focused.value = app
    }

    fun launch(app: AppEntry) {
        prefs.recordLaunch(app.packageName)
        start(app.launchIntent)
    }

    fun openMenu(app: AppEntry) {
        menuFor.value = app
    }

    fun openMenuForFocused() {
        if (menuFor.value == null) focused.value?.let { menuFor.value = it }
    }

    fun closeMenu() {
        menuFor.value = null
    }

    fun toggleFavorite(app: AppEntry) = prefs.toggleFavorite(app.packageName)

    fun moveFavorite(app: AppEntry, delta: Int) = prefs.moveFavorite(app.packageName, delta)

    /** Ricerca di Google TV: prima la ricerca globale (testo + voce), poi l'Assistente. */
    fun openSearch() {
        val pm = getApplication<Application>().packageManager
        val candidates = listOf(
            Intent("android.search.action.GLOBAL_SEARCH"),
            Intent(Intent.ACTION_ASSIST),
            Intent("android.intent.action.VOICE_ASSIST"),
            Intent(Intent.ACTION_SEARCH).setPackage("com.google.android.katniss"),
        )
        val intent = candidates.firstOrNull { pm.resolveActivity(it, 0) != null } ?: return
        start(intent)
    }

    fun openAppInfo(app: AppEntry) =
        start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")))

    fun uninstall(app: AppEntry) =
        start(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")))

    fun openSettings() = start(Intent(Settings.ACTION_SETTINGS))

    fun openInput(input: TvInput) = start(input.intent)

    fun splashDone() {
        android.util.Log.i("Lu3gSplash", "splashDone")
        _showSplash.value = false
        _splashRunning.value = false
    }

    /**
     * Arma la splash: schermo nero in attesa che il pannello sia acceso. Se nessuno
     * la avvia entro tre secondi parte da sola. Non tocca una splash già in corso.
     */
    fun armSplash() {
        if (_showSplash.value && _splashRunning.value) return
        android.util.Log.i("Lu3gSplash", "armSplash")
        _splashRunning.value = false
        _showSplash.value = true
        viewModelScope.launch {
            delay(3_000)
            if (_showSplash.value && !_splashRunning.value) {
                android.util.Log.i("Lu3gSplash", "startSplash (timeout)")
                _splashRunning.value = true
            }
        }
    }

    /** Avvia animazione e suono (schermo acceso). Idempotente. */
    fun startSplash() {
        if (_showSplash.value && _splashRunning.value) return
        android.util.Log.i("Lu3gSplash", "startSplash")
        _showSplash.value = true
        _splashRunning.value = true
    }

    fun onHomePressed() {
        _homeEvents.tryEmit(Unit)
    }

    private fun start(intent: Intent) {
        runCatching {
            getApplication<Application>().startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

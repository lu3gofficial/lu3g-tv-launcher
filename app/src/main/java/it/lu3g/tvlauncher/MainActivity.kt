package it.lu3g.tvlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import it.lu3g.tvlauncher.ui.LauncherScreen
import it.lu3g.tvlauncher.ui.theme.Lu3gTheme

private const val TAG = "Lu3gSplash"

/** Margine dopo "schermo acceso" prima di far partire splash e suono: il pannello finisce di accendersi. */
private const val PANEL_MARGIN_MS = 500L

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()

    /** Aggiorna la lista quando un'app viene installata, rimossa o aggiornata. */
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            vm.refresh()
        }
    }

    /**
     * Splash "di accensione": la vera animazione di boot non è modificabile senza root,
     * ma quando la TV si risveglia dallo standby sul launcher la splash viene rilanciata.
     * Lo standby si riconosce in due modi, perché l'ordine degli eventi varia tra TV:
     *  - l'activity viene fermata con lo schermo già spento (onStop con isInteractive false);
     *  - oppure arriva ACTION_SCREEN_ON mentre l'activity è ancora in primo piano.
     * Se la TV si riaccende su un'altra app, la splash non parte.
     */
    private var stoppedForStandby = false
    private val powerManager by lazy { getSystemService(Context.POWER_SERVICE) as PowerManager }
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.i(TAG, "broadcast ${intent?.action} stato=${lifecycle.currentState}")
            if (intent?.action == Intent.ACTION_SCREEN_ON && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                // Schermo acceso: nero per un attimo (il pannello finisce di accendersi), poi via
                vm.armSplash()
                lifecycleScope.launch {
                    delay(PANEL_MARGIN_MS)
                    vm.startSplash()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Lu3gTheme {
                LauncherScreen(vm)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        // Sono broadcast protetti del sistema: nessun'altra app può inviarli
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        ContextCompat.registerReceiver(this, screenReceiver, IntentFilter(Intent.ACTION_SCREEN_ON), ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onStart() {
        super.onStart()
        vm.refresh()
        Log.i(TAG, "onStart flag=$stoppedForStandby interactive=${powerManager.isInteractive}")
        if (stoppedForStandby) {
            stoppedForStandby = false
            // Il pannello si accende dopo: si parte allo SCREEN_ON (o dopo il timeout)
            vm.armSplash()
        }
    }

    override fun onStop() {
        // Schermo già spento quando veniamo fermati: è lo standby, non un'altra app
        stoppedForStandby = !powerManager.isInteractive
        Log.i(TAG, "onStop interactive=${powerManager.isInteractive} -> flag=$stoppedForStandby")
        super.onStop()
    }

    override fun onDestroy() {
        unregisterReceiver(screenReceiver)
        unregisterReceiver(packageReceiver)
        super.onDestroy()
    }

    /** Tasto Home premuto mentre il launcher è già in primo piano: torna in cima. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vm.onHomePressed()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            vm.openMenuForFocused()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

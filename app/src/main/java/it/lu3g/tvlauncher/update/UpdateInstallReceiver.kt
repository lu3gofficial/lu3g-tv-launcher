package it.lu3g.tvlauncher.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import android.widget.Toast
import it.lu3g.tvlauncher.R

/** Riceve lo stato della sessione di installazione: apre la conferma di sistema, segnala gli errori. */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
                context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PackageInstaller.STATUS_SUCCESS -> Log.i(TAG, "aggiornamento installato")
            else -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "stato $status"
                Log.w(TAG, "installazione non riuscita: $msg")
                Toast.makeText(context, context.getString(R.string.update_error, msg), Toast.LENGTH_LONG).show()
            }
        }
    }

    private companion object {
        const val TAG = "Lu3gUpdate"
    }
}

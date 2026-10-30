package it.lu3g.tvlauncher.update

/** Stato del processo di aggiornamento, mostrato nella finestra e nel chip in alto. */
sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data object NeedsPermission : UpdateState()
    data class Downloading(val progress: Float) : UpdateState()
    data object Installing : UpdateState()
    data class Error(val message: String) : UpdateState()
}

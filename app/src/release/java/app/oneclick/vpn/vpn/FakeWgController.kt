package app.oneclick.vpn.vpn

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FakeWgController(private val scope: CoroutineScope) : TunnelController {
  private val stateFlow = MutableStateFlow<TunnelState>(TunnelState.Disconnected)
  private var connectJob: Job? = null
  private var updaterJob: Job? = null

  override val state: StateFlow<TunnelState> = stateFlow

  override fun connect(context: Context) {
    if (stateFlow.value is TunnelState.Connecting || stateFlow.value is TunnelState.Connected) return
    connectJob?.cancel()
    updaterJob?.cancel()
    stateFlow.value = TunnelState.Connecting
    connectJob = scope.launch {
      delay(1200)
      var rx = 0L
      var tx = 0L
      stateFlow.value = TunnelState.Connected(rx, tx)
      updaterJob = scope.launch {
        while (true) {
          delay(1000)
          rx += 128 * 1024
          tx += 96 * 1024
          stateFlow.value = TunnelState.Connected(rx, tx)
        }
      }
    }
  }

  override fun disconnect() {
    connectJob?.cancel()
    updaterJob?.cancel()
    connectJob = null
    updaterJob = null
    stateFlow.value = TunnelState.Disconnected
  }
}

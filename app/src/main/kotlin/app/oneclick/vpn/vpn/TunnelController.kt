package app.oneclick.vpn.vpn

import kotlinx.coroutines.flow.StateFlow

sealed class TunnelState {
  data object Disconnected : TunnelState()
  data object Connecting : TunnelState()
  data class Connected(val receivedBytes: Long, val transmittedBytes: Long) : TunnelState()
  data class Error(val message: String) : TunnelState()
}

interface TunnelController {
  val state: StateFlow<TunnelState>
  fun connect(context: android.content.Context)
  fun disconnect()
  fun toggle(context: android.content.Context) {
    when (state.value) {
      is TunnelState.Disconnected, is TunnelState.Error -> connect(context)
      else -> disconnect()
    }
  }
}

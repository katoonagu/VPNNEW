package app.oneclick.vpn.vpn

import kotlinx.coroutines.flow.StateFlow

sealed class TunnelState {
  data object Disconnected : TunnelState()
  data object Connecting : TunnelState()
  data class Connected(val endpoint: String, val rxBytes: Long, val txBytes: Long) : TunnelState()
  data class Error(val message: String, val cause: Throwable? = null) : TunnelState()
}

interface TunnelController {
  val state: StateFlow<TunnelState>

  suspend fun connect(configText: String)

  suspend fun disconnect()
}

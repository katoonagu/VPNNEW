package app.oneclick.vpn.vpn

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

sealed class TunnelState {
    data object Disconnected : TunnelState()
    data object Connecting : TunnelState()
    data class Connected(val rxBytes: Long, val txBytes: Long) : TunnelState()
    data class Error(val message: String, val cause: Throwable? = null) : TunnelState()
}

interface TunnelController {
    val state: StateFlow<TunnelState>

    fun connect(context: Context)

    fun disconnect()

    fun dispose()
}

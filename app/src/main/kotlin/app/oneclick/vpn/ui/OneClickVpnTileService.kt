package app.oneclick.vpn.ui

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.oneclick.vpn.vpn.TunnelState
import app.oneclick.vpn.vpn.observeState
import app.oneclick.vpn.vpn.toggle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OneClickVpnTileService : TileService() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var stateJob: Job? = null

  override fun onStartListening() {
    super.onStartListening()
    val tile = qsTile ?: return
    stateJob?.cancel()
    stateJob = scope.launch {
      observeState().collectLatest { state ->
        tile.state = when (state) {
          is TunnelState.Connected -> Tile.STATE_ACTIVE
          is TunnelState.Connecting -> Tile.STATE_UNAVAILABLE
          else -> Tile.STATE_INACTIVE
        }
        tile.label = "OneClick VPN"
        tile.updateTile()
      }
    }
  }

  override fun onStopListening() {
    stateJob?.cancel()
    stateJob = null
    super.onStopListening()
  }

  override fun onClick() {
    super.onClick()
    toggle(this)
  }

  override fun onDestroy() {
    stateJob?.cancel()
    scope.cancel()
    super.onDestroy()
  }
}

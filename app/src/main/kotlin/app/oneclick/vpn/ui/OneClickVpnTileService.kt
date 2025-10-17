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

    private val tileScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateJob: Job? = null

    override fun onClick() {
        super.onClick()
        toggle(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        stateJob?.cancel()
        stateJob = tileScope.launch {
            observeState().collectLatest { state ->
                qsTile?.let { tile ->
                    when (state) {
                        TunnelState.Disconnected -> tile.state = Tile.STATE_INACTIVE
                        TunnelState.Connecting -> tile.state = Tile.STATE_UNAVAILABLE
                        is TunnelState.Connected -> tile.state = Tile.STATE_ACTIVE
                        is TunnelState.Error -> tile.state = Tile.STATE_INACTIVE
                    }
                    tile.stateDescription = when (state) {
                        TunnelState.Disconnected -> "Отключено"
                        TunnelState.Connecting -> "Подключение..."
                        is TunnelState.Connected -> "Подключено"
                        is TunnelState.Error -> "Ошибка"
                    }
                    tile.updateTile()
                }
            }
        }
    }

    override fun onStopListening() {
        stateJob?.cancel()
        stateJob = null
        super.onStopListening()
    }

    override fun onDestroy() {
        tileScope.cancel()
        super.onDestroy()
    }
}

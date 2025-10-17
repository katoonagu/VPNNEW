package app.oneclick.vpn.vpn

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import app.oneclick.vpn.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object VpnApi {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val internalState = MutableStateFlow<TunnelState>(TunnelState.Disconnected)
  private var controller: TunnelController? = null
  private var controllerJob: Job? = null

  fun observeState(): StateFlow<TunnelState> {
    ensureController()
    return internalState.asStateFlow()
  }

  fun connect(context: Context) {
    val activeController = ensureController()
    scope.launch {
      val config = loadDefaultConfig(context).getOrElse { error ->
        internalState.value = TunnelState.Error(error.message ?: "WireGuard config missing", error)
        return@launch
      }
      try {
        activeController.connect(config)
      } catch (t: Throwable) {
        internalState.value = TunnelState.Error(t.message ?: "Failed to connect", t)
      }
    }
  }

  fun disconnect() {
    val activeController = controller
    if (activeController == null) {
      internalState.value = TunnelState.Disconnected
      return
    }
    scope.launch {
      try {
        activeController.disconnect()
      } catch (t: Throwable) {
        internalState.value = TunnelState.Error(t.message ?: "Failed to disconnect", t)
      }
    }
  }

  @MainThread
  fun toggle(context: Context) {
    when (observeState().value) {
      is TunnelState.Connected,
      is TunnelState.Connecting -> disconnect()
      is TunnelState.Disconnected,
      is TunnelState.Error -> connect(context)
    }
  }

  fun ensureController(): TunnelController {
    val existing = controller
    if (existing != null) {
      return existing
    }
    val created = createStubController()
    controller = created
    controllerJob?.cancel()
    controllerJob = scope.launch {
      created.state.collect { internalState.value = it }
    }
    return created
  }

  fun loadDefaultConfig(context: Context): Result<String> = runCatching {
    val assetPath = BuildConfig.DEFAULT_WG_ASSET
    require(assetPath.isNotBlank()) { "DEFAULT_WG_ASSET is not configured" }
    context.assets.open(assetPath).bufferedReader().use { it.readText() }
  }.onFailure { error ->
    Log.w("OneClickVPN", "Failed to load default config", error)
  }

  private fun createStubController(): TunnelController {
    return try {
      Class.forName("app.oneclick.vpn.vpn.FakeWgController")
        .getDeclaredConstructor()
        .newInstance() as TunnelController
    } catch (ignored: Throwable) {
      StubTunnelController()
    }
  }
}

private class StubTunnelController : TunnelController {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val _state = MutableStateFlow<TunnelState>(TunnelState.Disconnected)
  override val state: StateFlow<TunnelState> = _state.asStateFlow()
  private var trafficJob: Job? = null

  override suspend fun connect(configText: String) {
    if (_state.value is TunnelState.Connecting || _state.value is TunnelState.Connected) {
      return
    }
    _state.value = TunnelState.Connecting
    delay(600)
    _state.value = TunnelState.Connected("stub.endpoint:51820", 0, 0)
    startTrafficLoop()
  }

  override suspend fun disconnect() {
    trafficJob?.cancel()
    trafficJob = null
    _state.value = TunnelState.Disconnected
  }

  private fun startTrafficLoop() {
    trafficJob?.cancel()
    trafficJob = scope.launch {
      var rx = 0L
      var tx = 0L
      while (isActive) {
        delay(750)
        val current = _state.value
        if (current is TunnelState.Connected) {
          rx += 32_768
          tx += 21_504
          _state.value = current.copy(rxBytes = rx, txBytes = tx)
        } else {
          break
        }
      }
    }
  }
}

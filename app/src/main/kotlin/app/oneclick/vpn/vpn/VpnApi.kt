package app.oneclick.vpn.vpn

import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException

object VpnApi {
    private val internalState = MutableStateFlow<TunnelState>(TunnelState.Disconnected)
    private var controller: TunnelController? = null

    fun observeState(): StateFlow<TunnelState> {
        ensureController()
        return internalState.asStateFlow()
    }

    fun connect(context: Context) {
        val activeController = ensureController()
        activeController.connect(context)
    }

    fun disconnect() {
        controller?.disconnect()
    }

    fun toggle(context: Context) {
        when (internalState.value) {
            TunnelState.Connected -> disconnect()
            TunnelState.Connecting -> disconnect()
            is TunnelState.Error -> disconnect()
            TunnelState.Disconnected -> connect(context)
        }
    }

    fun ensureController(): TunnelController {
        val existing = controller
        if (existing != null) {
            return existing
        }
        val created = createStubController()
        controller = created
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
            val clazz = Class.forName("app.oneclick.vpn.vpn.FakeWgController")
            val ctor = clazz.getConstructor(MutableStateFlow::class.java)
            ctor.newInstance(internalState) as TunnelController
        } catch (ignored: Throwable) {
            BaseFakeWgController(internalState)
        }
    }
}

internal open class BaseFakeWgController(
    private val tunnelState: MutableStateFlow<TunnelState>
) : TunnelController {

    override val state: StateFlow<TunnelState> = tunnelState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var trafficJob: Job? = null

    override fun connect(context: Context) {
        when (tunnelState.value) {
            TunnelState.Connecting, is TunnelState.Connected -> return
            else -> Unit
        }

        tunnelState.value = TunnelState.Connecting
        scope.launch {
            delay(connectionDelayMillis())
            tunnelState.value = TunnelState.Connected(rxBytes = 0L, txBytes = 0L)
            startTrafficLoop()
        }
    }

    override fun disconnect() {
        trafficJob?.cancel()
        trafficJob = null
        tunnelState.value = TunnelState.Disconnected
    }

    override fun dispose() {
        disconnect()
        scope.cancel()
    }

    protected open fun connectionDelayMillis(): Long = 600L

    protected open fun trafficIntervalMillis(): Long = 750L

    protected open fun nextSnapshot(current: TunnelState.Connected): TunnelState.Connected {
        val rxInc = 32_768L
        val txInc = 21_504L
        return current.copy(
            rxBytes = current.rxBytes + rxInc,
            txBytes = current.txBytes + txInc
        )
    }

    private fun startTrafficLoop() {
        trafficJob?.cancel()
        trafficJob = scope.launch {
            while (isActive) {
                delay(trafficIntervalMillis())
                val current = tunnelState.value
                if (current is TunnelState.Connected) {
                    tunnelState.value = nextSnapshot(current)
                } else {
                    break
                }
            }
        }
    }
}

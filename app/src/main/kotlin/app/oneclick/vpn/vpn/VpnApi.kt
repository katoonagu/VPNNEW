package app.oneclick.vpn.vpn

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicReference

object VpnApi {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val controllerRef = AtomicReference<TunnelController?>(null)
  private val state = MutableStateFlow<TunnelState>(TunnelState.Disconnected)
  private var collectionJob: Job? = null

  fun observeState(): StateFlow<TunnelState> = state.asStateFlow()

  fun ensureController(): TunnelController {
    val existing = controllerRef.get()
    if (existing != null) return existing
    val controller = FakeControllerFactory.create(scope)
    controllerRef.set(controller)
    collectionJob?.cancel()
    collectionJob = scope.launch {
      controller.state.collect { state.value = it }
    }
    return controller
  }

  fun connect(context: Context) {
    ensureController().connect(context)
  }

  fun disconnect() {
    controllerRef.get()?.disconnect()
  }

  fun toggle(context: Context) {
    ensureController().toggle(context)
  }

  fun loadDefaultConfig(context: Context, assetPath: String): String {
    val assetManager = context.assets
    return assetManager.open(assetPath).use { input ->
      BufferedReader(InputStreamReader(input)).readText()
    }
  }

  private object FakeControllerFactory {
    fun create(scope: CoroutineScope): TunnelController = FakeWgController(scope)
  }
}

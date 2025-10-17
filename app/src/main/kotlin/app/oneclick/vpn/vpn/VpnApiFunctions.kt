package app.oneclick.vpn.vpn

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

fun observeState(): StateFlow<TunnelState> = VpnApi.observeState()

fun connect(context: Context) = VpnApi.connect(context)

fun disconnect() = VpnApi.disconnect()

fun toggle(context: Context) = VpnApi.toggle(context)

fun ensureController(): TunnelController = VpnApi.ensureController()

fun loadDefaultConfig(context: Context, assetPath: String): String =
  VpnApi.loadDefaultConfig(context, assetPath)

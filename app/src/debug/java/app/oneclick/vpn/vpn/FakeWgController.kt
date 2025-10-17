package app.oneclick.vpn.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.random.Random

class FakeWgController(
    state: MutableStateFlow<TunnelState>
) : BaseFakeWgController(state) {

    private val random = Random(System.currentTimeMillis())

    override fun connectionDelayMillis(): Long = 400L

    override fun trafficIntervalMillis(): Long = 500L

    override fun nextSnapshot(current: TunnelState.Connected): TunnelState.Connected {
        val rxStep = random.nextLong(24_000L, 96_000L)
        val txStep = random.nextLong(16_000L, 64_000L)
        return current.copy(
            rxBytes = current.rxBytes + rxStep,
            txBytes = current.txBytes + txStep
        )
    }
}

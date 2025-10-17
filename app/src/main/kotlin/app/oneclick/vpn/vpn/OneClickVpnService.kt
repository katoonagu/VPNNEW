package app.oneclick.vpn.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.oneclick.vpn.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OneClickVpnService : android.net.VpnService() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val notificationId = 1
  private val channelId = "oneclick_vpn"

  override fun onCreate() {
    super.onCreate()
    createChannel()
    startForeground(notificationId, buildNotification(statusLabel(TunnelState.Disconnected)))
    serviceScope.launch {
      observeState().collectLatest { state ->
        NotificationManagerCompat.from(this@OneClickVpnService)
          .notify(notificationId, buildNotification(statusLabel(state)))
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    ensureController()
    return START_STICKY
  }

  override fun onRevoke() {
    super.onRevoke()
    disconnect()
  }

  override fun onDestroy() {
    serviceScope.cancel()
    stopForeground(STOP_FOREGROUND_REMOVE)
    super.onDestroy()
  }

  private fun statusLabel(state: TunnelState): String = when (state) {
    TunnelState.Connecting -> "Connecting"
    is TunnelState.Connected -> "Connected • ${state.endpoint} • RX ${state.rxBytes} • TX ${state.txBytes}"
    is TunnelState.Error -> "Error: ${state.message}"
    TunnelState.Disconnected -> "Disconnected"
  }

  private fun createChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        channelId,
        "OneClick VPN",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        setShowBadge(false)
        lockscreenVisibility = Notification.VISIBILITY_PRIVATE
      }
      val manager = getSystemService(NotificationManager::class.java)
      manager?.createNotificationChannel(channel)
    }
  }

  private fun buildNotification(status: String): Notification {
    val intent = Intent(this, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      intent,
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    return NotificationCompat.Builder(this, channelId)
      .setContentTitle("OneClick VLESS VPN")
      .setContentText(status)
      .setSmallIcon(android.R.drawable.stat_sys_download_done)
      .setContentIntent(pendingIntent)
      .setOngoing(true)
      .build()
  }
}

package app.oneclick.vpn.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import app.oneclick.vpn.ui.MainActivity

class OneClickVpnService : android.net.VpnService() {
  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    startForeground(NOTIFICATION_ID, buildNotification())
    return START_STICKY
  }

  override fun onDestroy() {
    stopForeground(STOP_FOREGROUND_REMOVE)
    super.onDestroy()
  }

  private fun buildNotification(): Notification {
    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
      PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag()
    )
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("OneClick VPN")
      .setContentText("Туннель активен")
      .setSmallIcon(android.R.drawable.stat_sys_vpn_ic)
      .setContentIntent(pendingIntent)
      .setOngoing(true)
      .build()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "OneClick VPN",
        NotificationManager.IMPORTANCE_LOW
      )
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.createNotificationChannel(channel)
    }
  }

  private fun pendingIntentImmutableFlag(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

  companion object {
    private const val CHANNEL_ID = "oneclick_vpn"
    private const val NOTIFICATION_ID = 101
  }
}

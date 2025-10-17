package app.oneclick.vpn.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.oneclick.vpn.vpn.TunnelState
import app.oneclick.vpn.vpn.observeState
import app.oneclick.vpn.vpn.toggle

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          OneClickVpnScreen()
        }
      }
    }
  }
}

@Composable
private fun OneClickVpnScreen() {
  val context = LocalContext.current
  val tunnelState by observeState().collectAsState()
  var host by rememberSaveable { mutableStateOf("") }
  var port by rememberSaveable { mutableStateOf("") }
  var uuid by rememberSaveable { mutableStateOf("") }
  var sni by rememberSaveable { mutableStateOf("") }
  var publicKey by rememberSaveable { mutableStateOf("") }
  var shortId by rememberSaveable { mutableStateOf("") }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp)
      .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "OneClick VLESS VPN",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold
    )
    val buttonLabel = when (tunnelState) {
      is TunnelState.Connected -> "Отключить"
      is TunnelState.Connecting -> "Подключение..."
      else -> "Подключить"
    }
    Button(
      onClick = { toggle(context) },
      enabled = tunnelState !is TunnelState.Connecting,
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
    ) {
      Text(buttonLabel)
    }

    TunnelStatus(tunnelState)

    OutlinedTextField(
      value = host,
      onValueChange = { host = it.trim() },
      label = { Text("Host") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
      value = port,
      onValueChange = { port = it.filter { ch -> ch.isDigit() }.take(5) },
      label = { Text("Port") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
      value = uuid,
      onValueChange = { uuid = it.trim() },
      label = { Text("UUID") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
      value = sni,
      onValueChange = { sni = it.trim() },
      label = { Text("SNI") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
      value = publicKey,
      onValueChange = { publicKey = it.trim() },
      label = { Text("Public Key") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
      value = shortId,
      onValueChange = { shortId = it.trim() },
      label = { Text("Short ID") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )

    val canImport = listOf(host, port, uuid, sni, publicKey, shortId).all { it.isNotBlank() }
    Button(
      onClick = {
        val uri = buildVlessUri(host, port, uuid, sni, publicKey, shortId)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
      },
      enabled = canImport,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text("Импорт VLESS")
    }
  }
}

@Composable
private fun TunnelStatus(state: TunnelState) {
  val statusText = when (state) {
    is TunnelState.Connected -> "Подключено"
    is TunnelState.Connecting -> "Подключение..."
    is TunnelState.Error -> "Ошибка: ${state.message}"
    is TunnelState.Disconnected -> "Отключено"
  }
  Text(statusText, style = MaterialTheme.typography.bodyLarge)
  if (state is TunnelState.Connected) {
    Text(
      text = "RX: ${formatBytes(state.receivedBytes)} | TX: ${formatBytes(state.transmittedBytes)}",
      style = MaterialTheme.typography.bodyMedium
    )
  }
}

private fun buildVlessUri(host: String, port: String, uuid: String, sni: String, publicKey: String, shortId: String): String =
  "vless://$uuid@$host:$port?security=reality&sni=$sni&pbk=$publicKey&sid=$shortId&flow=xtls-rprx-vision&type=tcp#OneClick"

private fun formatBytes(bytes: Long): String {
  if (bytes < 1024) return "$bytes B"
  val kb = bytes / 1024.0
  if (kb < 1024) return String.format("%.1f KB", kb)
  val mb = kb / 1024.0
  return String.format("%.1f MB", mb)
}


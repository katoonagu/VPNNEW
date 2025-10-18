package app.oneclick.vpn.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.oneclick.vpn.help.ClaimActivity
import app.oneclick.vpn.vpn.TunnelState
import app.oneclick.vpn.vpn.observeState
import app.oneclick.vpn.vpn.toggle

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          MainScreen()
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen() {
  val context = LocalContext.current
  val vpnState by observeState().collectAsState(initial = TunnelState.Disconnected)

  var host by remember { mutableStateOf("") }
  var port by remember { mutableStateOf("443") }
  var uuid by remember { mutableStateOf("") }
  var sni by remember { mutableStateOf("") }
  var publicKey by remember { mutableStateOf("") }
  var shortId by remember { mutableStateOf("") }

  val isConnected = vpnState is TunnelState.Connected
  val buttonLabel = if (isConnected) "Отключить" else "Подключить"
  val statusText = when (val state = vpnState) {
    TunnelState.Disconnected -> "Статус: отключено"
    TunnelState.Connecting -> "Статус: подключение..."
    is TunnelState.Connected -> "Статус: ${state.endpoint} (RX ${state.rxBytes} • TX ${state.txBytes})"
    is TunnelState.Error -> "Ошибка: ${state.message}"
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "OneClick VLESS VPN",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold
    )
    Text(text = statusText, style = MaterialTheme.typography.bodyLarge)

    Button(
      onClick = { toggle(context) },
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(text = buttonLabel)
    }

    Button(
      onClick = { context.startActivity(Intent(context, ClaimActivity::class.java)) },
      modifier = Modifier.fillMaxWidth()
    ) {
      Text("Загрузить config QR")
    }

    OutlinedTextField(
      value = host,
      onValueChange = { host = it },
      label = { Text("Host") },
      modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
      value = port,
      onValueChange = { port = it.filter { ch -> ch.isDigit() } },
      label = { Text("Port") },
      modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
      value = uuid,
      onValueChange = { uuid = it },
      label = { Text("UUID") },
      modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
      value = sni,
      onValueChange = { sni = it },
      label = { Text("SNI") },
      modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
      value = publicKey,
      onValueChange = { publicKey = it },
      label = { Text("Public Key") },
      modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
      value = shortId,
      onValueChange = { shortId = it },
      label = { Text("Short ID") },
      modifier = Modifier.fillMaxWidth()
    )

    Button(
      onClick = {
        if (host.isBlank() || port.isBlank() || uuid.isBlank() || sni.isBlank() || publicKey.isBlank() || shortId.isBlank()) {
          Toast.makeText(context, "Заполните все поля", Toast.LENGTH_SHORT).show()
        } else {
          val uri = buildVlessUri(host, port, uuid, sni, publicKey, shortId)
          val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
          if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
          } else {
            Toast.makeText(context, "Нет приложения для импорта", Toast.LENGTH_SHORT).show()
          }
        }
      },
      modifier = Modifier.fillMaxWidth()
    ) {
      Text("Импорт VLESS")
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = "Конфиг по умолчанию: ${app.oneclick.vpn.BuildConfig.DEFAULT_WG_ASSET}",
      style = MaterialTheme.typography.bodySmall
    )
  }
}

private fun buildVlessUri(
  host: String,
  port: String,
  uuid: String,
  sni: String,
  publicKey: String,
  shortId: String
): String {
  return "vless://$uuid@$host:$port?security=reality&sni=$sni&pbk=$publicKey&sid=$shortId&flow=xtls-rprx-vision&type=tcp#OneClick"
}

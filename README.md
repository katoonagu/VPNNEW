# OneClick VLESS VPN

Android demo client that pairs a Compose UI with an Android `VpnService` entry point. Debug builds ship with a fake WireGuard controller that animates tunnel traffic so UX can be validated without a backend.

## Features
- One-tap toggle to connect or disconnect the VLESS tunnel.
- Quick Settings tile mirrors the main button for rapid access.
- Flavor matrix (`client01` … `client30`) with per-client default WireGuard asset binding.
- Gradle utilities to generate, verify, and enforce WireGuard asset and security policies.

## VLESS Import Workflow
1. Fill in Host, Port, UUID, SNI, Public Key, and Short ID in the debug UI.
2. Tap **Import VLESS**.
3. The app builds a `vless://` URI and fires an `Intent.ACTION_VIEW` targeting apps like v2rayNG or sing-box.

## WireGuard Assets
- Debug stub files live under `app/src/main/assets/wg/` and can be regenerated with `./gradlew generateWgStubs`.
- Replace individual `clientXX.conf` files with real WireGuard configs for production usage.
- Keep private keys outside of VCS; the `.gitignore` excludes `app/src/main/assets/wg/*.conf` by default.

## VPS Checklist
- Valid UUID
- Server reachable on port 443 (TCP)
- Public Key exported from Reality server
- SNI aligned with certificate/front domain
- Short ID configured on the server side

Debug builds remain in DEMO mode; release builds expect real tunnel integration.

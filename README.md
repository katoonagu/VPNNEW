# OneClick VLESS VPN

OneClick VLESS VPN — демо-приложение для Android, показывающее базовый UI на Jetpack Compose и интеграцию с `VpnService`. В debug-сборке используется демонстрационный туннель, эмулирующий подключение и передачу данных без реального WireGuard.

## Возможности
- Экран с большой кнопкой «Подключить/Отключить» и отображением статистики RX/TX.
- Поля для ввода параметров VLESS (host, port, UUID, SNI, publicKey, shortId) и кнопка «Импорт VLESS», открывающая `Intent.ACTION_VIEW` для клиентов вроде v2rayNG или sing-box.
- 30 product flavors (`client01`…`client30`), переключающих конфиг WireGuard по умолчанию.
- Сервис быстрой настройки (Quick Settings Tile) для быстрого переключения туннеля.
- Кастомные Gradle-задачи `generateWgStubs`, `verifyWgAssets` и `verifySecurityConfig`.

## DEMO-режим
В debug-профиле включён `BuildConfig.DEMO=true`, поэтому запускается `FakeWgController`, который переходит в состояние Connected и генерирует счётчики RX/TX. Release-профиль временно использует ту же заглушку.

## WireGuard конфиги
Файлы-заглушки создаются задачей `generateWgStubs` в `app/src/main/assets/wg`. Реальные `.conf` поместите в эту же папку, имя должно соответствовать flavor-у (например, `client07.conf`). Сами конфиги добавлены в `.gitignore`.

## Импорт VLESS
Введите host, port, UUID, SNI, publicKey и shortId, после чего нажмите «Импорт VLESS». Приложение сформирует ссылку вида:
```
vless://UUID@HOST:PORT?security=reality&sni=SNI&pbk=PUBLIC_KEY&sid=SHORT_ID&flow=xtls-rprx-vision&type=tcp#OneClick
```
и откроет Intent для совместимых клиентов.

## Мини-чеклист для VPS
- Сгенерируйте уникальный UUID.
- Настройте publicKey (WireGuard) и соответствующий privateKey на сервере.
- Подтвердите корректный SNI для Reality.
- Назначьте shortId.
- Убедитесь, что порт 443 (или указанный вами) открыт.

## Bootstrap Gradle
При отсутствии wrapper выполните `bootstrap-gradle.ps1`, который скачает Gradle 8.9 в `%USERPROFILE%\gradle` и создаст wrapper.

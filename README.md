# AppLock

Schütze deine Privatsphäre mit einer Mustersperre oder PIN für einzelne Apps.

![](art/ic_launcher.png)

---

## Übersicht

**AppLock** ist eine Android-App, mit der du einzelne Anwendungen auf deinem Gerät mit einem Entsperrmuster oder einer numerischen PIN sichern kannst. Beim Öffnen einer gesperrten App wird der Nutzer zunächst zur Eingabe des Passworts aufgefordert.

### Funktionen

- Einzelne Apps mit Mustersperre oder PIN absichern
- Automatische Sperre nach einer konfigurierbaren Zeitspanne
- Sofortige Sperre nach Bildschirmabschaltung
- Automatische Fotoaufnahme bei fehlgeschlagenen Entsperrversuchen
- Suche nach Apps in der App-Liste

### Voraussetzungen

- Android 5.0 (API 21) oder höher
- Berechtigung **„App-Nutzungsdaten abrufen"** erforderlich

---

## Installation & Einrichtung

### Entwicklungsvoraussetzungen

- [Android Studio](https://developer.android.com/studio) (aktuelle Version empfohlen)
- Android SDK (mindestens API 21)
- Java Development Kit (JDK) 11 oder höher

### Projekt klonen

```bash
git clone https://github.com/d-schoenfeld/AppLock.git
cd AppLock
```

### Projekt öffnen

1. Android Studio starten
2. **„Open an existing Android Studio project"** auswählen
3. Den geklonten Projektordner `AppLock` öffnen
4. Gradle-Synchronisation abwarten

### App bauen und starten

1. Gerät oder Emulator verbinden (Android 5.0+)
2. In Android Studio auf **Run ▶** klicken oder folgenden Befehl im Terminal ausführen:

```bash
./gradlew assembleDebug
```

Die APK-Datei wird anschließend unter `app/build/outputs/apk/debug/` abgelegt.

### Benötigte Berechtigungen

Beim ersten Start der App wird ein Dialog zur Vergabe der Berechtigung **„App-Nutzungsdaten abrufen"** angezeigt. Diese Berechtigung ist notwendig, damit AppLock erkennen kann, welche App gerade im Vordergrund läuft.

---

## Import & Export der Einstellungen via ADB

AppLock bietet die Möglichkeit, alle Einstellungen als JSON-Datei zu exportieren und auf ein anderes Gerät zu übertragen – vollständig über die ADB-Kommandozeile ohne Verwendung der UI.

### Voraussetzungen

- ADB-Debugging auf dem Gerät aktiviert
- AppLock auf dem Gerät installiert und mindestens einmal gestartet

### Geschützte Schnittstelle

Der Import/Export-Receiver ist durch die Systemberechtigung `android.permission.DUMP` geschützt. Diese Berechtigung besitzen nur der ADB-Shell-Nutzer (uid 2000) und System-Apps – reguläre Nutzer-Apps haben keinen Zugriff.

### Export

Exportiert alle Einstellungen in eine JSON-Datei:

```bash
# Export in das Standard-App-Verzeichnis
adb shell am broadcast -a com.lzx.lock.EXPORT_SETTINGS

# Export in einen benutzerdefinierten Pfad
adb shell am broadcast -a com.lzx.lock.EXPORT_SETTINGS \
    --es path /sdcard/Android/data/com.lzx.lock/files/applock_settings.json

# Datei auf den PC kopieren
adb pull /sdcard/Android/data/com.lzx.lock/files/applock_settings.json
```

### Import

Importiert Einstellungen von einer JSON-Datei:

```bash
# Datei auf das Gerät kopieren
adb push applock_settings.json /sdcard/Android/data/com.lzx.lock/files/applock_settings.json

# Import auslösen
adb shell am broadcast -a com.lzx.lock.IMPORT_SETTINGS \
    --es path /sdcard/Android/data/com.lzx.lock/files/applock_settings.json
```

### Übertragung zwischen Geräten

```bash
# 1. Auf Quellgerät exportieren und Datei holen
adb -s <QUELLE> shell am broadcast -a com.lzx.lock.EXPORT_SETTINGS
adb -s <QUELLE> pull /sdcard/Android/data/com.lzx.lock/files/applock_settings.json

# 2. Datei auf Zielgerät übertragen und importieren
adb -s <ZIEL> push applock_settings.json /sdcard/Android/data/com.lzx.lock/files/applock_settings.json
adb -s <ZIEL> shell am broadcast -a com.lzx.lock.IMPORT_SETTINGS \
    --es path /sdcard/Android/data/com.lzx.lock/files/applock_settings.json
```

### Inhalt der JSON-Datei

Die exportierte Datei enthält folgende Informationen:

| Feld | Beschreibung |
|---|---|
| `settings.app_lock_state` | AppLock aktiviert/deaktiviert |
| `settings.lock_method` | Sperrmethode (`pin` oder `pattern`) |
| `settings.lock_pin_hash` | SHA-256-Hash des PINs/Passworts |
| `settings.lock_auto_screen` | Sperre nach Bildschirmabschaltung |
| `settings.lock_auto_screen_time` | Zeitverzögerung für Auto-Sperre |
| `settings.lock_apart_milliseconds` | Zeitabstand in Millisekunden |
| `settings.lock_apart_title` | Anzeigename des Zeitabstands |
| `settings.AutoRecordPic` | Automatische Fotoaufnahme aktiviert |
| `settings.AutoRecordPicAttempt` | Anzahl der Fehlversuche vor Fotoaufnahme (Standard: 1) |
| `settings.lock_is_hide_line` | Muster ausblenden |
| `gesture_pattern_hex` | Muster-Hash als Hex-String (nur bei Mustersperre) |
| `locked_apps` | Liste der gesperrten App-Paketnamen |

> **Hinweis:** Der PIN-Hash wird als SHA-256-Wert exportiert – das ursprüngliche Passwort kann daraus nicht abgeleitet werden. Nach dem Import ist dasselbe Passwort wie auf dem Quellgerät gültig.

---

## Verwendete Bibliotheken

- [LitePal](https://github.com/LitePalFramework/LitePal) – SQLite-Datenbankverwaltung

---

## Screenshots

<a href="art/1.png"><img src="art/1.png" width="30%"/></a>
<a href="art/2.png"><img src="art/2.png" width="30%"/></a>
<a href="art/3.png"><img src="art/3.png" width="30%"/></a>

<a href="art/4.png"><img src="art/4.png" width="30%"/></a>
<a href="art/5.png"><img src="art/5.png" width="30%"/></a>
<a href="art/6.png"><img src="art/6.png" width="30%"/></a>

<a href="art/7.png"><img src="art/7.png" width="30%"/></a>
<a href="art/8.png"><img src="art/8.png" width="30%"/></a>
<a href="art/9.png"><img src="art/9.png" width="30%"/></a>

<a href="art/10.png"><img src="art/10.png" width="30%"/></a>
<a href="art/11.png"><img src="art/11.png" width="30%"/></a>

---

## Lizenz

```
Copyright 2017 L_Xian

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

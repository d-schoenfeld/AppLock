# AppLock – App-Sperre

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

- Android 4.0 (API 14) oder höher
- Auf Android 5.0 (Lollipop) und höher: Berechtigung **„App-Nutzungsdaten abrufen"** erforderlich

---

## Installation & Einrichtung

### Entwicklungsvoraussetzungen

- [Android Studio](https://developer.android.com/studio) (aktuelle Version empfohlen)
- Android SDK (mindestens API 14)
- Java Development Kit (JDK) 8 oder höher

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

1. Gerät oder Emulator verbinden (Android 4.0+)
2. In Android Studio auf **Run ▶** klicken oder folgenden Befehl im Terminal ausführen:

```bash
./gradlew assembleDebug
```

Die APK-Datei wird anschließend unter `app/build/outputs/apk/debug/` abgelegt.

### Benötigte Berechtigungen

Beim ersten Start der App wird (auf Android 5.0+) ein Dialog zur Vergabe der Berechtigung **„App-Nutzungsdaten abrufen"** angezeigt. Diese Berechtigung ist notwendig, damit AppLock erkennen kann, welche App gerade im Vordergrund läuft.

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

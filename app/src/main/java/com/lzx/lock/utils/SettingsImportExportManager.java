package com.lzx.lock.utils;

import android.content.ContentValues;
import android.content.Context;
import android.text.TextUtils;

import com.lzx.lock.base.AppConstants;
import com.lzx.lock.bean.CommLockInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Verwaltet den Import und Export aller AppLock-Einstellungen als JSON.
 * Exportiert werden: Sperrstatus, Sperrmethode, PIN-Hash, Musterdatei,
 * Auto-Screen-Lock, Zeitabstand, Fotoaufnahme, Muster-Verstecken sowie
 * die Liste der gesperrten Apps.
 */
public class SettingsImportExportManager {

    /** Aktuelle Exportformat-Version – bei inkompatiblen Änderungen erhöhen. */
    private static final int EXPORT_VERSION = 1;

    /**
     * Exportiert alle Einstellungen in ein JSON-String.
     *
     * @param context Anwendungskontext
     * @return hübsch formatierter JSON-String
     */
    public static String exportToJson(Context context) throws JSONException, IOException {
        JSONObject root = new JSONObject();
        root.put("version", EXPORT_VERSION);

        // SharedPreferences-Einstellungen
        JSONObject settings = new JSONObject();
        SpUtil sp = SpUtil.getInstance();
        settings.put(AppConstants.LOCK_STATE, sp.getBoolean(AppConstants.LOCK_STATE));
        settings.put(AppConstants.LOCK_AUTO_SCREEN, sp.getBoolean(AppConstants.LOCK_AUTO_SCREEN, false));
        settings.put(AppConstants.LOCK_AUTO_RECORD_PIC, sp.getBoolean(AppConstants.LOCK_AUTO_RECORD_PIC, false));
        settings.put(AppConstants.LOCK_IS_HIDE_LINE, sp.getBoolean(AppConstants.LOCK_IS_HIDE_LINE, false));
        settings.put(AppConstants.LOCK_METHOD, sp.getString(AppConstants.LOCK_METHOD, ""));
        settings.put(AppConstants.LOCK_PIN_HASH, sp.getString(AppConstants.LOCK_PIN_HASH, ""));
        settings.put(AppConstants.LOCK_APART_TITLE, sp.getString(AppConstants.LOCK_APART_TITLE, "Sofort"));
        settings.put(AppConstants.LOCK_APART_MILLISENCONS, sp.getLong(AppConstants.LOCK_APART_MILLISENCONS));
        settings.put(AppConstants.LOCK_AUTO_SCREEN_TIME, sp.getBoolean(AppConstants.LOCK_AUTO_SCREEN_TIME, false));
        root.put("settings", settings);

        // Muster-Datei als Hex exportieren (SHA-1-Hash, gespeichert in gesture.key)
        File patternFile = new File(context.getFilesDir(), "gesture.key");
        if (patternFile.exists() && patternFile.length() > 0) {
            root.put("gesture_pattern_hex", bytesToHex(readBinaryFile(patternFile)));
        }

        // Gesperrte Apps aus der Datenbank
        JSONArray lockedApps = new JSONArray();
        List<CommLockInfo> allApps = LitePal.findAll(CommLockInfo.class);
        for (CommLockInfo app : allApps) {
            if (app.isLocked()) {
                lockedApps.put(app.getPackageName());
            }
        }
        root.put("locked_apps", lockedApps);

        return root.toString(2);
    }

    /**
     * Importiert Einstellungen aus einem JSON-String und überschreibt die aktuellen Werte.
     *
     * @param context Anwendungskontext
     * @param json    JSON-String, der zuvor mit {@link #exportToJson} erzeugt wurde
     */
    public static void importFromJson(Context context, String json) throws JSONException, IOException {
        JSONObject root = new JSONObject(json);

        // SharedPreferences-Einstellungen wiederherstellen
        JSONObject settings = root.optJSONObject("settings");
        if (settings != null) {
            SpUtil sp = SpUtil.getInstance();
            if (settings.has(AppConstants.LOCK_STATE))
                sp.putBoolean(AppConstants.LOCK_STATE, settings.getBoolean(AppConstants.LOCK_STATE));
            if (settings.has(AppConstants.LOCK_AUTO_SCREEN))
                sp.putBoolean(AppConstants.LOCK_AUTO_SCREEN, settings.getBoolean(AppConstants.LOCK_AUTO_SCREEN));
            if (settings.has(AppConstants.LOCK_AUTO_RECORD_PIC))
                sp.putBoolean(AppConstants.LOCK_AUTO_RECORD_PIC, settings.getBoolean(AppConstants.LOCK_AUTO_RECORD_PIC));
            if (settings.has(AppConstants.LOCK_IS_HIDE_LINE))
                sp.putBoolean(AppConstants.LOCK_IS_HIDE_LINE, settings.getBoolean(AppConstants.LOCK_IS_HIDE_LINE));
            if (settings.has(AppConstants.LOCK_METHOD))
                sp.putString(AppConstants.LOCK_METHOD, settings.getString(AppConstants.LOCK_METHOD));
            if (settings.has(AppConstants.LOCK_PIN_HASH))
                sp.putString(AppConstants.LOCK_PIN_HASH, settings.getString(AppConstants.LOCK_PIN_HASH));
            if (settings.has(AppConstants.LOCK_APART_TITLE))
                sp.putString(AppConstants.LOCK_APART_TITLE, settings.getString(AppConstants.LOCK_APART_TITLE));
            if (settings.has(AppConstants.LOCK_APART_MILLISENCONS))
                sp.putLong(AppConstants.LOCK_APART_MILLISENCONS, settings.getLong(AppConstants.LOCK_APART_MILLISENCONS));
            if (settings.has(AppConstants.LOCK_AUTO_SCREEN_TIME))
                sp.putBoolean(AppConstants.LOCK_AUTO_SCREEN_TIME, settings.getBoolean(AppConstants.LOCK_AUTO_SCREEN_TIME));
        }

        // Muster-Datei wiederherstellen
        String patternHex = root.optString("gesture_pattern_hex", "");
        if (!TextUtils.isEmpty(patternHex)) {
            File patternFile = new File(context.getFilesDir(), "gesture.key");
            writeBinaryFile(patternFile, hexToBytes(patternHex));
        }

        // Gesperrte Apps wiederherstellen
        JSONArray lockedAppsJson = root.optJSONArray("locked_apps");
        if (lockedAppsJson != null) {
            List<String> lockedPkgs = new ArrayList<>();
            for (int i = 0; i < lockedAppsJson.length(); i++) {
                lockedPkgs.add(lockedAppsJson.getString(i));
            }

            // Für den Fall, dass die Datenbank noch nicht initialisiert ist (Erststart):
            // Liste in SharedPreferences zwischenspeichern und bei DB-Initialisierung anwenden.
            SpUtil.getInstance().putString(AppConstants.LOCK_IMPORTED_APPS,
                    TextUtils.join(",", lockedPkgs));

            // Vorhandene DB-Einträge sofort aktualisieren
            ContentValues unlockValues = new ContentValues();
            unlockValues.put("isLocked", 0);
            LitePal.updateAll(CommLockInfo.class, unlockValues);

            ContentValues lockValues = new ContentValues();
            lockValues.put("isLocked", 1);
            for (String pkg : lockedPkgs) {
                LitePal.updateAll(CommLockInfo.class, lockValues, "packageName = ?", pkg);
            }
        }
    }

    // ---- Hilfsmethoden ----

    private static byte[] readBinaryFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        try {
            byte[] bytes = new byte[(int) file.length()];
            int read = fis.read(bytes);
            if (read != bytes.length) {
                throw new IOException("Datei konnte nicht vollständig gelesen werden: " + file);
            }
            return bytes;
        } finally {
            fis.close();
        }
    }

    private static void writeBinaryFile(File file, byte[] data) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        try {
            fos.write(data);
        } finally {
            fos.close();
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Ungültiger Hex-String (ungerade Länge)");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("Ungültiges Hex-Zeichen an Position " + i);
            }
            data[i / 2] = (byte) ((hi << 4) + lo);
        }
        return data;
    }
}

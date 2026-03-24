package com.lzx.lock.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.lzx.lock.base.AppConstants;
import com.lzx.lock.service.LockService;
import com.lzx.lock.utils.SettingsImportExportManager;
import com.lzx.lock.utils.SpUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * BroadcastReceiver für den Import und Export der AppLock-Einstellungen via ADB.
 *
 * Dieser Receiver ist durch die Berechtigung {@code android.permission.DUMP} geschützt,
 * die regulären Nutzer-Apps nicht besitzen. Der ADB-Shell-Nutzer (uid 2000) verfügt
 * über diese Berechtigung, sodass die Befehle ausschließlich per ADB ausführbar sind.
 *
 * <h3>Export (Einstellungen auf SD-Karte schreiben):</h3>
 * <pre>
 * adb shell am broadcast -a com.lzx.lock.EXPORT_SETTINGS \
 *     --es path /sdcard/Android/data/com.lzx.lock/files/applock_settings.json
 * </pre>
 *
 * <h3>Import (Einstellungen von Datei laden):</h3>
 * <pre>
 * adb shell am broadcast -a com.lzx.lock.IMPORT_SETTINGS \
 *     --es path /sdcard/Android/data/com.lzx.lock/files/applock_settings.json
 * </pre>
 *
 * Ohne {@code --es path ...} wird der Standard-Pfad
 * {@code /sdcard/Android/data/com.lzx.lock/files/applock_settings.json} verwendet.
 */
public class ImportExportReceiver extends BroadcastReceiver {

    /** Intent-Action für den Export. */
    public static final String ACTION_EXPORT = "com.lzx.lock.EXPORT_SETTINGS";
    /** Intent-Action für den Import. */
    public static final String ACTION_IMPORT = "com.lzx.lock.IMPORT_SETTINGS";
    /** Optionaler Intent-Extra mit dem absoluten Dateipfad. */
    public static final String EXTRA_PATH = "path";

    private static final String TAG = "AppLock.ImportExport";
    private static final String DEFAULT_FILENAME = "applock_settings.json";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        String path = intent.getStringExtra(EXTRA_PATH);

        if (ACTION_EXPORT.equals(action)) {
            handleExport(context, path);
        } else if (ACTION_IMPORT.equals(action)) {
            handleImport(context, path);
        }
    }

    private void handleExport(Context context, String path) {
        File outputFile = resolveFile(context, path);
        try {
            File dir = outputFile.getParentFile();
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
            String json = SettingsImportExportManager.exportToJson(context);
            writeTextFile(outputFile, json);
            Log.i(TAG, "Einstellungen exportiert nach: " + outputFile.getAbsolutePath());
            setResultCode(0);
            setResultData("Export erfolgreich: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Export fehlgeschlagen", e);
            setResultCode(1);
            setResultData("Export fehlgeschlagen: " + e.getMessage());
        }
    }

    private void handleImport(Context context, String path) {
        File inputFile = resolveFile(context, path);
        if (!inputFile.exists()) {
            String msg = "Import-Datei nicht gefunden: " + inputFile.getAbsolutePath();
            Log.e(TAG, msg);
            setResultCode(2);
            setResultData(msg);
            return;
        }
        try {
            String json = readTextFile(inputFile);
            SettingsImportExportManager.importFromJson(context, json);

            // LockService neu starten, damit geänderte Einstellungen übernommen werden
            restartLockService(context);

            Log.i(TAG, "Einstellungen importiert von: " + inputFile.getAbsolutePath());
            setResultCode(0);
            setResultData("Import erfolgreich von: " + inputFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Import fehlgeschlagen", e);
            setResultCode(3);
            setResultData("Import fehlgeschlagen: " + e.getMessage());
        }
    }

    /**
     * Löst den Dateipfad auf. Wenn kein Pfad angegeben wurde, wird das externe
     * App-Verzeichnis verwendet (kein WRITE_EXTERNAL_STORAGE erforderlich ab API 19).
     */
    private File resolveFile(Context context, String path) {
        if (!TextUtils.isEmpty(path)) {
            return new File(path);
        }
        File dir = context.getExternalFilesDir(null);
        if (dir == null) {
            dir = context.getFilesDir();
        }
        return new File(dir, DEFAULT_FILENAME);
    }

    private void restartLockService(Context context) {
        Intent lockServiceIntent = new Intent(context, LockService.class);
        context.stopService(lockServiceIntent);
        if (SpUtil.getInstance().getBoolean(AppConstants.LOCK_STATE)) {
            context.startService(lockServiceIntent);
        }
    }

    private static void writeTextFile(File file, String content) throws IOException {
        FileWriter writer = new FileWriter(file);
        try {
            writer.write(content);
        } finally {
            writer.close();
        }
    }

    private static String readTextFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } finally {
            reader.close();
        }
        return sb.toString();
    }
}

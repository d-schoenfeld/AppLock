package com.lzx.lock.base;

/**
 * Created by xian on 2017/2/17.
 */

public class AppConstants {

    public static final String LOCK_STATE = "app_lock_state"; //AppLock-Schalter (Status: true=ein, false=aus)
    public static final String LOCK_FAVITER_NUM = "lock_faviter_num"; //Anzahl empfohlener zu sperrenden Apps
    public static final String LOCK_SYS_APP_NUM = "lock_sys_app_num"; //Anzahl System-Apps
    public static final String LOCK_USER_APP_NUM = "lock_user_app_num"; //Anzahl Nicht-System-Apps
    public static final String LOCK_IS_INIT_FAVITER = "lock_is_init_faviter"; //ob Faviter-Datentabelle initialisiert
    public static final String LOCK_IS_INIT_DB = "lock_is_init_db"; //ob Datenbanktabelle initialisiert
    public static final String APP_PACKAGE_NAME = "com.lzx.lock"; //Paketname
    public static final String LOCK_IS_HIDE_LINE = "lock_is_hide_line"; //ob Muster verborgen
    public static final String LOCK_PWD = "lock_pwd";//AppLock-Passwort
    public static final String LOCK_IS_FIRST_LOCK = "is_lock"; //ob bereits gesperrt wurde
    public static final String LOCK_AUTO_SCREEN = "lock_auto_screen"; //ob nach Bildschirmabschaltung erneut sperren
    public static final String LOCK_AUTO_SCREEN_TIME = "lock_auto_screen_time"; //ob nach Bildschirmabschaltung nach einer Zeitspanne erneut gesperrt werden soll
    public static final String LOCK_CURR_MILLISENCONS = "lock_curr_milliseconds"; //aktuelle Zeit speichern (Millisekunden)
    public static final String LOCK_APART_MILLISENCONS = "lock_apart_milliseconds"; //Zeitabstand speichern (Millisekunden)
    public static final String LOCK_APART_TITLE = "lock_apart_title"; //Titel des gespeicherten Zeitabstands
    public static final String LOCK_LAST_LOAD_PKG_NAME = "last_load_package_name";
    public static final String LOCK_PACKAGE_NAME = "lock_package_name"; //Paketname der geöffneten gesperrten App
    public static final String LOCK_FROM = "lock_from"; //Action nach der Entsperrung
    public static final String LOCK_FROM_FINISH = "lock_from_finish"; //Action nach Entsperrung ist finish
    public static final String LOCK_FROM_SETTING = "lock_from_setting"; //Action nach Entsperrung ist setting
    public static final String LOCK_FROM_UNLOCK = "lock_from_unlock"; //Action nach der Entsperrung
    public static final String LOCK_FROM_LOCK_MAIN_ACITVITY = "lock_from_lock_main_activity";
    public static final String LOCK_AUTO_RECORD_PIC = "AutoRecordPic"; //ob automatische Fotoaufnahme aktiviert
    public static final String LOCK_AUTO_RECORD_PIC_ATTEMPT = "AutoRecordPicAttempt"; //Anzahl der Fehlversuche vor Fotoaufnahme
    public static final String LOCK_METHOD = "lock_method"; // Sperrmethode ("pin" oder "pattern")
    public static final String LOCK_METHOD_PIN = "pin"; // Sperrmethode: Passwort/PIN
    public static final String LOCK_METHOD_PATTERN = "pattern"; // Sperrmethode: Muster
    public static final String LOCK_PIN_HASH = "lock_pin_hash"; // SHA-256-Hash des Passworts/PINs
    public static final String LOCK_IMPORTED_APPS = "lock_imported_apps"; // kommagetrennte Paketliste aus Import (für DB-Initialisierung)
}

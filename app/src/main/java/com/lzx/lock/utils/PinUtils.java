package com.lzx.lock.utils;

import android.text.TextUtils;

import com.lzx.lock.base.AppConstants;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hilfklasse für Passwort/PIN: Hashing, Speichern und Prüfen
 */
public class PinUtils {

    public static final int MIN_PIN_LENGTH = 4;

    /**
     * Berechnet den SHA-256-Hash des PINs/Passworts.
     * Schlägt bei fehlendem Algorithmus fehl (SHA-256 ist auf Android immer verfügbar).
     */
    private static String hashPin(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pin.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            // SHA-256 und UTF-8 sind auf Android immer verfügbar; bei Fehler sicher abbrechen
            throw new RuntimeException("PIN-Hashing fehlgeschlagen", e);
        }
    }

    /**
     * Speichert den PIN/Passwort-Hash in den SharedPreferences
     */
    public static void savePin(String pin) {
        SpUtil.getInstance().putString(AppConstants.LOCK_PIN_HASH, hashPin(pin));
    }

    /**
     * Prüft, ob der eingegebene PIN/Passwort dem gespeicherten Hash entspricht.
     * Gibt false zurück, wenn kein PIN gespeichert ist (kein Bypass möglich).
     */
    public static boolean checkPin(String pin) {
        String stored = SpUtil.getInstance().getString(AppConstants.LOCK_PIN_HASH, "");
        if (TextUtils.isEmpty(stored)) return false;
        return stored.equals(hashPin(pin));
    }

    /**
     * Prüft, ob ein PIN/Passwort gespeichert ist
     */
    public static boolean savedPinExists() {
        return !TextUtils.isEmpty(SpUtil.getInstance().getString(AppConstants.LOCK_PIN_HASH, ""));
    }

    /**
     * Löscht den gespeicherten PIN/Passwort-Hash
     */
    public static void clearPin() {
        SpUtil.getInstance().putString(AppConstants.LOCK_PIN_HASH, "");
    }

    /**
     * Gibt die aktuell gespeicherte Sperrmethode zurück.
     * Rückwärtskompatibel: wenn nicht gesetzt, wird Muster zurückgegeben (Bestandsnutzer).
     * Für neue Nutzer wird beim Einrichten explizit eine Methode gesetzt (Standard: PIN).
     */
    public static String getLockMethod() {
        String method = SpUtil.getInstance().getString(AppConstants.LOCK_METHOD, "");
        if (TextUtils.isEmpty(method)) {
            // Bestandsnutzer ohne gespeicherte Methode: Muster annehmen (Rückwärtskompatibilität)
            return AppConstants.LOCK_METHOD_PATTERN;
        }
        return method;
    }

    /**
     * Prüft, ob die aktuelle Sperrmethode PIN/Passwort ist
     */
    public static boolean isPinMethod() {
        return AppConstants.LOCK_METHOD_PIN.equals(getLockMethod());
    }
}

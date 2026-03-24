package com.lzx.lock.service;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import java.util.concurrent.atomic.AtomicBoolean;

import com.lzx.lock.R;
import com.lzx.lock.base.AppConstants;
import com.lzx.lock.db.CommLockInfoManager;
import com.lzx.lock.module.lock.GestureUnlockActivity;
import com.lzx.lock.receiver.ServiceRestartReceiver;
import com.lzx.lock.utils.SpUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xian on 2017/2/17.
 */

public class LockService extends Service {

    private AtomicBoolean mIsServiceDestroyed = new AtomicBoolean(false);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }


    public static final String UNLOCK_ACTION = "UNLOCK_ACTION";
    public static final String LOCK_SERVICE_LASTTIME = "LOCK_SERVICE_LASTTIME";
    public static final String LOCK_SERVICE_LASTAPP = "LOCK_SERVICE_LASTAPP";

    /** Notification-Kanal-ID für den Vordergrunddienst */
    public static final String NOTIFICATION_CHANNEL_ID = "lock_service_channel";
    /** Notification-ID für den Vordergrunddienst */
    private static final int NOTIFICATION_ID = 1001;


    private long lastUnlockTimeSeconds = 0; //Zeitpunkt der letzten Entsperrung
    private String lastUnlockPackageName = ""; //Paketname der zuletzt entsperrten App

    private boolean lockState;

    private ServiceReceiver mServiceReceiver;
    private CommLockInfoManager mLockInfoManager;
    private ActivityManager activityManager;

    public static boolean isActionLock = false;
    public String savePkgName;

    // Globale statische Variablen als Cache für häufig gelesene SharedPreferences-Werte
    public static volatile boolean sLockAutoScreen = false;
    public static volatile boolean sLockAutoScreenTime = false;
    public static volatile long sLockCurrMilliseconds = 0;
    public static volatile long sLockApartMilliseconds = 0;
    public static volatile String sLastLoadPkgName = "";

    @Override
    public void onCreate() {
        super.onCreate();
        lockState = SpUtil.getInstance().getBoolean(AppConstants.LOCK_STATE);
        // Statische Variablen beim Start aus den SharedPreferences initialisieren
        sLockAutoScreen = SpUtil.getInstance().getBoolean(AppConstants.LOCK_AUTO_SCREEN, false);
        sLockAutoScreenTime = SpUtil.getInstance().getBoolean(AppConstants.LOCK_AUTO_SCREEN_TIME, false);
        sLockCurrMilliseconds = SpUtil.getInstance().getLong(AppConstants.LOCK_CURR_MILLISENCONS, 0);
        sLockApartMilliseconds = SpUtil.getInstance().getLong(AppConstants.LOCK_APART_MILLISENCONS, 0);
        sLastLoadPkgName = SpUtil.getInstance().getString(AppConstants.LOCK_LAST_LOAD_PKG_NAME, "");
        mLockInfoManager = new CommLockInfoManager(this);
        activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

        // Als Vordergrunddienst starten, damit Android den Dienst nicht beendet
        startForegroundWithNotification();

        //Broadcast registrieren
        mServiceReceiver = new ServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(UNLOCK_ACTION);
        registerReceiver(mServiceReceiver, filter);

        //Thread zum Prüfen der Bildschirmsperre starten
        mIsServiceDestroyed.set(false);
        AsyncTask.SERIAL_EXECUTOR.execute(new ServiceWorker());

    }

    private class ServiceWorker implements Runnable {
        @Override
        public void run() {
            checkData();
        }
    }

    /**
     * Vordergrunddienst-Benachrichtigung erstellen und Dienst in den Vordergrund bringen.
     * Verhindert, dass Android den Dienst bei Speichermangel beendet.
     */
    private void startForegroundWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(R.string.lock_service_notification_channel),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.lock_service_notification_title))
                .setContentText(getString(R.string.lock_service_notification_text))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    /**
     * Neustart des Dienstes über AlarmManager planen.
     * Wird nach onDestroy() und onTaskRemoved() aufgerufen, damit der Dienst
     * nach einer unerwarteten Beendigung automatisch neu gestartet wird.
     */
    private void scheduleServiceRestart() {
        if (!SpUtil.getInstance().getBoolean(AppConstants.LOCK_STATE, false)) {
            return;
        }
        Intent restartIntent = new Intent(getApplicationContext(), ServiceRestartReceiver.class);
        restartIntent.setPackage(getPackageName());
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getApplicationContext(), 1, restartIntent, flags);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                        || alarmManager.canScheduleExactAlarms();
                if (canScheduleExact) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            SystemClock.elapsedRealtime() + 1000, pendingIntent);
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            SystemClock.elapsedRealtime() + 1000, pendingIntent);
                }
            } else {
                alarmManager.set(AlarmManager.ELAPSED_REALTIME,
                        SystemClock.elapsedRealtime() + 1000, pendingIntent);
            }
        }
    }

    private void checkData() {
        while (!mIsServiceDestroyed.get()) {
            //Paketname der vordersten App ermitteln
            String packageName = getLauncherTopApp(LockService.this, activityManager);

            //Entsperrseite anhand des Paketnamens öffnen
            if (lockState && !inWhiteList(packageName) && !TextUtils.isEmpty(packageName)) {
                boolean isLockOffScreenTime = sLockAutoScreenTime; //ob temporäres Verlassen aktiv
                boolean isLockOffScreen = sLockAutoScreen; //ob nach Bildschirmabschaltung erneut sperren
                savePkgName = sLastLoadPkgName;
                //Log.i("Server", "packageName = " + packageName + "  savePkgName = " + savePkgName);
                //Fall 1: Erst nach einer Zeitspanne nach der Entsperrung wieder sperren
                if (isLockOffScreenTime && !isLockOffScreen) {
                    long time = sLockCurrMilliseconds; //gespeicherte Zeit abrufen
                    long leaverTime = sLockApartMilliseconds; //Abwesenheitszeit abrufen
                    if (!TextUtils.isEmpty(savePkgName)) {
                        if (!TextUtils.isEmpty(packageName)) {
                            if (!savePkgName.equals(packageName)) { //
                                if (getHomes().contains(packageName) || packageName.contains("launcher")) {
                                    boolean isSetUnLock = mLockInfoManager.isSetUnLock(savePkgName);
                                    if (!isSetUnLock) {
                                        if (System.currentTimeMillis() - time > leaverTime) {
                                            mLockInfoManager.lockCommApplication(savePkgName);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                //Fall 2: Nach Entsperrung ohne Bildschirmabschaltung: nach Verlassen der App nach einer Zeitspanne sperren
                if (isLockOffScreenTime && isLockOffScreen) {
                    long time = sLockCurrMilliseconds; //gespeicherte Zeit abrufen
                    long leaverTime = sLockApartMilliseconds; //Abwesenheitszeit abrufen
                    if (!TextUtils.isEmpty(savePkgName)) {
                        if (!TextUtils.isEmpty(packageName)) {
                            if (!savePkgName.equals(packageName)) {
                                if (getHomes().contains(packageName) || packageName.contains("launcher")) {
                                    boolean isSetUnLock = mLockInfoManager.isSetUnLock(savePkgName);
                                    if (!isSetUnLock) {
                                        if (System.currentTimeMillis() - time > leaverTime) {
                                            mLockInfoManager.lockCommApplication(savePkgName);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                //Fall 3: Sofort nach Bildschirmabschaltung sperren, auch nach Verlassen sperren
                if (!isLockOffScreenTime && isLockOffScreen) {
                    if (!TextUtils.isEmpty(savePkgName)) {
                        if (!TextUtils.isEmpty(packageName)) {
                            if (!savePkgName.equals(packageName)) {
                                isActionLock = false;
                                if (getHomes().contains(packageName) || packageName.contains("launcher")) {
                                    boolean isSetUnLock = mLockInfoManager.isSetUnLock(savePkgName);
                                    if (!isSetUnLock) {
                                        mLockInfoManager.lockCommApplication(savePkgName);
                                    }
                                }
                            } else {
                                isActionLock = true;
                            }
                        }
                    }
                }

                //Fall 4: Immer sperren
                if (!isLockOffScreenTime && !isLockOffScreen) {
                    if (!TextUtils.isEmpty(savePkgName)) {
                        if (!TextUtils.isEmpty(packageName)) {
                            if (!savePkgName.equals(packageName)) {
                                if (getHomes().contains(packageName) || packageName.contains("launcher")) {
                                    boolean isSetUnLock = mLockInfoManager.isSetUnLock(savePkgName);
                                    if (!isSetUnLock) {
                                        mLockInfoManager.lockCommApplication(savePkgName);
                                    }
                                }
                            }
                        }
                    }
                }

                // Verschiedene Sperren suchen und Logik prüfen
                if (mLockInfoManager.isLockedPackageName(packageName)) {
                    passwordLock(packageName);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                } else {

                }
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Whitelist
     */
    private boolean inWhiteList(String packageName) {
        return packageName.equals(AppConstants.APP_PACKAGE_NAME);
    }

    /**
     * Dienst-Broadcast
     */
    public class ServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            boolean isLockOffScreen = sLockAutoScreen; //ob nach Bildschirmabschaltung erneut sperren
            boolean isLockOffScreenTime = sLockAutoScreenTime; //ob nach Bildschirmabschaltung Zeit-basierte Sperrung aktiv

            switch (action) {
                case UNLOCK_ACTION:  //Broadcast nach Entsperrung
                    lastUnlockPackageName = intent.getStringExtra(LOCK_SERVICE_LASTAPP); //Paketname der zuletzt entsperrten App
                    lastUnlockTimeSeconds = intent.getLongExtra(LOCK_SERVICE_LASTTIME, lastUnlockTimeSeconds); //Zeitpunkt der letzten Entsperrung
                    break;
                case Intent.ACTION_SCREEN_OFF: //Broadcast bei Bildschirmabschaltung
                    long screenOffTime = System.currentTimeMillis();
                    SpUtil.getInstance().putLong(AppConstants.LOCK_CURR_MILLISENCONS, screenOffTime); //Zeitpunkt der Bildschirmabschaltung speichern
                    sLockCurrMilliseconds = screenOffTime; //Statische Variable aktualisieren
                    //Fall 3
                    if (!isLockOffScreenTime && isLockOffScreen) {
                        String savePkgName = sLastLoadPkgName;
                        if (!TextUtils.isEmpty(savePkgName)) {
                            if (isActionLock) {
                                mLockInfoManager.lockCommApplication(lastUnlockPackageName);
                            }
                        }
                    }
                    break;
            }
        }
    }

    /**
     * Paketname der vordersten App ermitteln
     */
    public String getLauncherTopApp(Context context, ActivityManager activityManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            List<ActivityManager.RunningTaskInfo> appTasks = activityManager.getRunningTasks(1);
            if (null != appTasks && !appTasks.isEmpty()) {
                return appTasks.get(0).topActivity.getPackageName();
            }
        } else {
            //ab Android 5.0 diese Methode verwenden
            UsageStatsManager sUsageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            long endTime = System.currentTimeMillis();
            long beginTime = endTime - 60000; // 60 Sekunden zurückblicken für zuverlässige Erkennung
            String result = "";
            UsageEvents.Event event = new UsageEvents.Event();
            UsageEvents usageEvents = sUsageStatsManager.queryEvents(beginTime, endTime);
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event);
                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    result = event.getPackageName();
                }
            }
            if (!android.text.TextUtils.isEmpty(result)) {
                return result;
            }
        }
        return "";
    }

    /**
     * Paketnamen der Launcher-Apps ermitteln
     */
    private List<String> getHomes() {
        List<String> names = new ArrayList<>();
        PackageManager packageManager = this.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo ri : resolveInfo) {
            names.add(ri.activityInfo.packageName);
        }
        return names;
    }

    /**
     * Entsperrbildschirm als Activity starten.
     * Durch Verwendung einer Activity statt eines Overlays funktioniert die Sperre
     * auch für System-Apps wie "Einstellungen", die Overlays über HIDE_OVERLAY_WINDOWS
     * ausblenden können (Android 12+).
     */
    private void passwordLock(String packageName) {
        if (!GestureUnlockActivity.isShowing) {
            Intent intent = new Intent(LockService.this, GestureUnlockActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra(AppConstants.LOCK_PACKAGE_NAME, packageName);
            intent.putExtra(AppConstants.LOCK_FROM, AppConstants.LOCK_FROM_FINISH);
            startActivity(intent);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // Neustart planen, wenn der Nutzer die App aus der Aufgabenliste entfernt
        scheduleServiceRestart();
    }

    @Override
    public void onDestroy() {
        mIsServiceDestroyed.set(true);
        super.onDestroy();
        unregisterReceiver(mServiceReceiver);
        // Neustart planen, damit der Dienst nach unerwarteter Beendigung wieder startet
        scheduleServiceRestart();
    }
}

package com.lzx.lock.service;

import android.app.ActivityManager;
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
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.concurrent.atomic.AtomicBoolean;

import com.lzx.lock.LockApplication;
import com.lzx.lock.base.AppConstants;
import com.lzx.lock.db.CommLockInfoManager;
import com.lzx.lock.module.lock.GestureUnlockActivity;
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


    private long lastUnlockTimeSeconds = 0; //Zeitpunkt der letzten Entsperrung
    private String lastUnlockPackageName = ""; //Paketname der zuletzt entsperrten App

    private boolean lockState;

    private ServiceReceiver mServiceReceiver;
    private CommLockInfoManager mLockInfoManager;
    private ActivityManager activityManager;

    public static boolean isActionLock = false;
    public String savePkgName;

    @Override
    public void onCreate() {
        super.onCreate();
        lockState = SpUtil.getInstance().getBoolean(AppConstants.LOCK_STATE);
        mLockInfoManager = new CommLockInfoManager(this);
        activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

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

    private void checkData() {
        while (!mIsServiceDestroyed.get()) {
            //Paketname der vordersten App ermitteln
            String packageName = getLauncherTopApp(LockService.this, activityManager);

            //Entsperrseite anhand des Paketnamens öffnen
            if (lockState && !inWhiteList(packageName) && !TextUtils.isEmpty(packageName)) {
                boolean isLockOffScreenTime = SpUtil.getInstance().getBoolean(AppConstants.LOCK_AUTO_SCREEN_TIME, false); //ob temporäres Verlassen aktiv
                boolean isLockOffScreen = SpUtil.getInstance().getBoolean(AppConstants.LOCK_AUTO_SCREEN, false); //ob nach Bildschirmabschaltung erneut sperren
                savePkgName = SpUtil.getInstance().getString(AppConstants.LOCK_LAST_LOAD_PKG_NAME, "");
                //Log.i("Server", "packageName = " + packageName + "  savePkgName = " + savePkgName);
                //Fall 1: Erst nach einer Zeitspanne nach der Entsperrung wieder sperren
                if (isLockOffScreenTime && !isLockOffScreen) {
                    long time = SpUtil.getInstance().getLong(AppConstants.LOCK_CURR_MILLISENCONS, 0); //gespeicherte Zeit abrufen
                    long leaverTime = SpUtil.getInstance().getLong(AppConstants.LOCK_APART_MILLISENCONS, 0); //Abwesenheitszeit abrufen
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
                    long time = SpUtil.getInstance().getLong(AppConstants.LOCK_CURR_MILLISENCONS, 0); //gespeicherte Zeit abrufen
                    long leaverTime = SpUtil.getInstance().getLong(AppConstants.LOCK_APART_MILLISENCONS, 0); //Abwesenheitszeit abrufen
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
                    continue;
                } else {

                }
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Whitelist
     */
    private boolean inWhiteList(String packageName) {
        return packageName.equals(AppConstants.APP_PACKAGE_NAME)
                || packageName.equals("com.android.settings");
    }

    /**
     * Dienst-Broadcast
     */
    public class ServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            boolean isLockOffScreen = SpUtil.getInstance().getBoolean(AppConstants.LOCK_AUTO_SCREEN, false); //ob nach Bildschirmabschaltung erneut sperren
            boolean isLockOffScreenTime = SpUtil.getInstance().getBoolean(AppConstants.LOCK_AUTO_SCREEN_TIME, false); //ob nach Bildschirmabschaltung Zeit-basierte Sperrung aktiv

            switch (action) {
                case UNLOCK_ACTION:  //Broadcast nach Entsperrung
                    lastUnlockPackageName = intent.getStringExtra(LOCK_SERVICE_LASTAPP); //Paketname der zuletzt entsperrten App
                    lastUnlockTimeSeconds = intent.getLongExtra(LOCK_SERVICE_LASTTIME, lastUnlockTimeSeconds); //Zeitpunkt der letzten Entsperrung
                    break;
                case Intent.ACTION_SCREEN_OFF: //Broadcast bei Bildschirmabschaltung
                    SpUtil.getInstance().putLong(AppConstants.LOCK_CURR_MILLISENCONS, System.currentTimeMillis()); //Zeitpunkt der Bildschirmabschaltung speichern
                    //Fall 3
                    if (!isLockOffScreenTime && isLockOffScreen) {
                        String savePkgName = SpUtil.getInstance().getString(AppConstants.LOCK_LAST_LOAD_PKG_NAME, "");
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
            long beginTime = endTime - 10000;
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
     * Zur Entsperrseite wechseln
     */
    private void passwordLock(String packageName) {
        LockApplication.getInstance().clearAllActivity();
        Intent intent = new Intent(this, GestureUnlockActivity.class);

        intent.putExtra(AppConstants.LOCK_PACKAGE_NAME, packageName);
        intent.putExtra(AppConstants.LOCK_FROM, AppConstants.LOCK_FROM_FINISH);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        mIsServiceDestroyed.set(true);
        super.onDestroy();
        unregisterReceiver(mServiceReceiver);
    }
}

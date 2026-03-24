package com.lzx.lock.helper;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;

import com.lzx.lock.base.AppConstants;
import com.lzx.lock.bean.CommLockInfo;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

/**
 * Hilfsklasse zum Laden der App-Liste
 */
public class LoadAppHelper {

    /**
     * Alle installierten Apps abrufen
     */
    private static List<ResolveInfo> loadPhoneAppList(PackageManager packageManager) {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        return packageManager.queryIntentActivities(intent, 0);
    }

    /**
     * Empfohlene zu sperrende Apps initialisieren
     */
    private static List<String> loadRecommendApps() {
        List<String> packages = new ArrayList<>();
        packages.add("com.android.gallery3d");       //Galerie
        packages.add("com.android.mms");             //SMS
        packages.add("com.tencent.mm");              //WeChat
        packages.add("com.android.contacts");        //Kontakte und Telefon
        packages.add("com.facebook.katana");         //Facebook
        packages.add("com.facebook.orca");           //Facebook Messenger
        packages.add("com.mediatek.filemanager");    //Dateimanager
        packages.add("com.sec.android.gallery3d");   //weitere Galerie-App
        packages.add("com.android.email");           //E-Mail
        packages.add("com.sec.android.app.myfiles"); //Samsung Dateien
        packages.add("com.android.vending");         //App-Store
        packages.add("com.google.android.youtube");  //YouTube
        packages.add("com.tencent.mobileqq");        //QQ
        packages.add("com.tencent.qq");              //QQ
        packages.add("com.android.dialer");          //Telefon
        packages.add("com.twitter.android");         //Twitter
        return packages;
    }

    /**
     * App-Informationen in benötigte Datenstruktur umwandeln
     */
    private static List<CommLockInfo> loadLockAppInfo(Activity activity) {
        List<CommLockInfo> list = new ArrayList<>();
        try {
            PackageManager mPackageManager = activity.getPackageManager();
            List<ResolveInfo> resolveInfos = loadPhoneAppList(mPackageManager);
            for (ResolveInfo resolveInfo : resolveInfos) {
                String packageName = resolveInfo.activityInfo.packageName;
                boolean isRecommend = isRecommendApp(packageName);
                CommLockInfo info = new CommLockInfo(packageName, false, isRecommend);
                ApplicationInfo appInfo = mPackageManager.getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
                String appName = mPackageManager.getApplicationLabel(appInfo).toString();
                if (!isFilterOutApps(packageName)) {
                    boolean isSysApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                    info.setLocked(isRecommend);
                    info.setAppName(appName);
                    info.setSysApp(isSysApp);
                    info.setTopTitle(isSysApp ? "System-Apps" : "Benutzer-Apps");
                    info.setSetUnLock(false);
                    info.setAppInfo(appInfo);
                    list.add(info);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Alle Apps asynchron laden
     */
    public static Observable<List<CommLockInfo>> loadAllLockAppInfoAsync(final Activity activity) {
        return Observable.create(new ObservableOnSubscribe<List<CommLockInfo>>() {
            @Override
            public void subscribe(ObservableEmitter<List<CommLockInfo>> emitter) throws Exception {
                emitter.onNext(loadLockAppInfo(activity));
            }
        });
    }

    /**
     * Gesperrte Apps asynchron laden
     */
    public static Observable<List<CommLockInfo>> loadLockedAppInfoAsync(final Activity activity) {
        return Observable.create(new ObservableOnSubscribe<List<CommLockInfo>>() {
            @Override
            public void subscribe(ObservableEmitter<List<CommLockInfo>> emitter) throws Exception {
                List<CommLockInfo> list = loadLockAppInfo(activity);
                List<CommLockInfo> lockAppInfos = new ArrayList<>();
                for (CommLockInfo info : list) {
                    if (info.isLocked()) {
                        lockAppInfos.add(info);
                    }
                }
                emitter.onNext(lockAppInfos);
            }
        });
    }

    /**
     * Entsperrte Apps asynchron laden
     */
    public static Observable<List<CommLockInfo>> loadUnLockAppInfoAsync(final Activity activity) {
        return Observable.create(new ObservableOnSubscribe<List<CommLockInfo>>() {
            @Override
            public void subscribe(ObservableEmitter<List<CommLockInfo>> emitter) throws Exception {
                List<CommLockInfo> list = loadLockAppInfo(activity);
                List<CommLockInfo> lockAppInfos = new ArrayList<>();
                for (CommLockInfo info : list) {
                    if (!info.isLocked()) {
                        lockAppInfos.add(info);
                    }
                }
                emitter.onNext(lockAppInfos);
            }
        });
    }

    /**
     * System-Apps asynchron laden
     */
    public static Observable<List<CommLockInfo>> loadSystemAppInfoAsync(final Activity activity) {
        return Observable.create(new ObservableOnSubscribe<List<CommLockInfo>>() {
            @Override
            public void subscribe(ObservableEmitter<List<CommLockInfo>> emitter) throws Exception {
                List<CommLockInfo> list = loadLockAppInfo(activity);
                List<CommLockInfo> lockAppInfos = new ArrayList<>();
                for (CommLockInfo info : list) {
                    if (info.isSysApp()) {
                        lockAppInfos.add(info);
                    }
                }
                emitter.onNext(lockAppInfos);
            }
        });
    }

    /**
     * Benutzer-Apps asynchron laden
     */
    public static Observable<List<CommLockInfo>> loadUserAppInfoAsync(final Activity activity) {
        return Observable.create(new ObservableOnSubscribe<List<CommLockInfo>>() {
            @Override
            public void subscribe(ObservableEmitter<List<CommLockInfo>> emitter) throws Exception {
                List<CommLockInfo> list = loadLockAppInfo(activity);
                List<CommLockInfo> lockAppInfos = new ArrayList<>();
                for (CommLockInfo info : list) {
                    if (!info.isSysApp()) {
                        lockAppInfos.add(info);
                    }
                }
                emitter.onNext(lockAppInfos);
            }
        });
    }

    /**
     * Prüfen ob empfohlene zu sperrende App
     */
    private static boolean isRecommendApp(String packageName) {
        List<String> packages = loadRecommendApps();
        return !TextUtils.isEmpty(packageName) && packages.contains(packageName);
    }

    /**
     * Whitelist für auszuschließende Apps
     */
    private static boolean isFilterOutApps(String packageName) {
        return packageName.equals(AppConstants.APP_PACKAGE_NAME) ||
                packageName.equals("com.google.android.googlequicksearchbox");
    }
}

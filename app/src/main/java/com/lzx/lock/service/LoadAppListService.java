package com.lzx.lock.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.lzx.lock.base.AppConstants;
import com.lzx.lock.bean.CommLockInfo;
import com.lzx.lock.bean.FaviterInfo;
import com.lzx.lock.db.CommLockInfoManager;
import com.lzx.lock.utils.SpUtil;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * App-Liste im Hintergrund laden
 * Created by xian on 2017/2/17.
 */

public class LoadAppListService extends IntentService {

    public static final String ACTION_START_LOAD_APP = "com.lzx.lock.service.action.LOADAPP";
    private PackageManager mPackageManager;
    private CommLockInfoManager mLockInfoManager;
    long time = 0;

    public LoadAppListService() {
        super("LoadAppListService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPackageManager = getPackageManager();
        mLockInfoManager = new CommLockInfoManager(this);
    }

    @Override
    protected void onHandleIntent(Intent handleIntent) {

        time = System.currentTimeMillis();

        boolean isInitFaviter = SpUtil.getInstance().getBoolean(AppConstants.LOCK_IS_INIT_FAVITER, false);
        boolean isInitDb = SpUtil.getInstance().getBoolean(AppConstants.LOCK_IS_INIT_DB, false);
        if (!isInitFaviter) {
            SpUtil.getInstance().putBoolean(AppConstants.LOCK_IS_INIT_FAVITER, true);
            initFavoriteApps();
        }

        //Alle installierten Apps bei jedem Start abrufen
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = mPackageManager.queryIntentActivities(intent, 0);
        //Nicht der erste Start, Daten vergleichen
        if (isInitDb) {
            List<ResolveInfo> appList = new ArrayList<>();
            List<CommLockInfo> dbList = mLockInfoManager.getAllCommLockInfos(); //Datenbankliste abrufen
            //App-Liste verarbeiten
            for (ResolveInfo resolveInfo : resolveInfos) {
                if (!resolveInfo.activityInfo.packageName.equals(AppConstants.APP_PACKAGE_NAME)) {
                    appList.add(resolveInfo);
                }
            }
            // Eindeutige Paketnamen bestimmen (ein Paket kann mehrere Launcher-Aktivitäten haben)
            java.util.Set<String> uniquePackageNames = new java.util.HashSet<>();
            for (ResolveInfo info : appList) {
                uniquePackageNames.add(info.activityInfo.packageName);
            }
            int uniqueAppCount = uniquePackageNames.size();
            if (uniqueAppCount > dbList.size()) { //wenn neue App installiert wurde
                List<ResolveInfo> reslist = new ArrayList<>();
                HashMap<String, CommLockInfo> hashMap = new HashMap<>();
                for (CommLockInfo info : dbList) {
                    hashMap.put(info.getPackageName(), info);
                }
                java.util.Set<String> addedPackages = new java.util.HashSet<>();
                for (ResolveInfo info : appList) {
                    String pkg = info.activityInfo.packageName;
                    if (!hashMap.containsKey(pkg) && addedPackages.add(pkg)) {
                        reslist.add(info);
                    }
                }
                try {
                    if (reslist.size() != 0)
                        mLockInfoManager.instanceCommLockInfoTable(reslist); //verbleibende neue Einträge in Datenbank einfügen
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            } else if (uniqueAppCount < dbList.size()) { //wenn App deinstalliert wurde
                List<CommLockInfo> commlist = new ArrayList<>();
                HashMap<String, ResolveInfo> hashMap = new HashMap<>();
                for (ResolveInfo info : appList) {
                    hashMap.put(info.activityInfo.packageName, info);
                }
                for (CommLockInfo info : dbList) {
                    if (!hashMap.containsKey(info.getPackageName())) {
                        commlist.add(info);
                    }
                }
                //Logger.d("App deinstalliert, Anzahl = " + dbList.size());
                if (commlist.size() != 0)
                    mLockInfoManager.deleteCommLockInfoTable(commlist);//überschüssige Einträge aus Datenbank löschen
            } else {
                //Logger.d("Keine Änderung bei den Apps, normal");
            }
        } else {
            //Datenbank nur einmal befüllen
            SpUtil.getInstance().putBoolean(AppConstants.LOCK_IS_INIT_DB, true);
            try {
                mLockInfoManager.instanceCommLockInfoTable(resolveInfos);    //in Datenbank einfügen
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        // Log.i("onHandleIntent", "Dauer = " + (System.currentTimeMillis() - time));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLockInfoManager = null;
    }

    /**
     * Standardmäßig zu sperrende Apps initialisieren
     */
    public void initFavoriteApps() {
        List<String> packageList = new ArrayList<>();
        List<FaviterInfo> faviterInfos = new ArrayList<>();
        packageList.add("com.android.gallery3d");       //Galerie
        packageList.add("com.android.mms");             //SMS
        packageList.add("com.tencent.mm");              //WeChat
        packageList.add("com.android.contacts");        //Kontakte und Telefon
        packageList.add("com.facebook.katana");         //facebook
        packageList.add("com.facebook.orca");           //facebook Messenger
        packageList.add("com.mediatek.filemanager");    //Dateimanager
        packageList.add("com.sec.android.gallery3d");   //weitere Galerie-App
        packageList.add("com.android.email");           //E-Mail
        packageList.add("com.sec.android.app.myfiles"); //Samsung Dateien
        packageList.add("com.android.vending");         //App-Store
        packageList.add("com.google.android.youtube");  //youtube
        packageList.add("com.tencent.mobileqq");        //qq
        packageList.add("com.tencent.qq");              //qq
        packageList.add("com.android.dialer");          //Telefon
        packageList.add("com.twitter.android");         //twitter
        for (String packageName : packageList) {
            FaviterInfo info = new FaviterInfo();
            info.setPackageName(packageName);
            faviterInfos.add(info);
        }

        LitePal.deleteAll(FaviterInfo.class);
        LitePal.saveAll(faviterInfos);
    }
}

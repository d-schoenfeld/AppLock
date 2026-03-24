package com.lzx.lock.db;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;

import com.lzx.lock.base.AppConstants;
import com.lzx.lock.bean.CommLockInfo;
import com.lzx.lock.bean.FaviterInfo;
import com.lzx.lock.utils.DataUtil;
import com.lzx.lock.utils.SpUtil;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.litepal.LitePal.where;

/**
 * Created by xian on 2017/2/17.
 */

public class CommLockInfoManager {

    private PackageManager mPackageManager;
    private Context mContext;

    public CommLockInfoManager(Context mContext) {
        this.mContext = mContext;
        mPackageManager = mContext.getPackageManager();
    }

    /**
     * Alle suchen
     */
    public synchronized List<CommLockInfo> getAllCommLockInfos() {
        List<CommLockInfo> commLockInfos = LitePal.findAll(CommLockInfo.class);
        Collections.sort(commLockInfos, commLockInfoComparator);
        return commLockInfos;
    }

    /**
     * Daten löschen
     */
    public synchronized void deleteCommLockInfoTable(List<CommLockInfo> commLockInfos) {
        for (CommLockInfo info : commLockInfos) {
            LitePal.deleteAll(CommLockInfo.class, "packageName = ?", info.getPackageName());
        }
    }

    /**
     * App-Informationen in Datenbank einfügen
     */
    public synchronized void instanceCommLockInfoTable(List<ResolveInfo> resolveInfos) throws PackageManager.NameNotFoundException {
        List<CommLockInfo> list = new ArrayList<>();

        // Importierte gesperrte Apps prüfen (gesetzt beim JSON-Import vor DB-Initialisierung)
        String importedAppsStr = SpUtil.getInstance().getString(AppConstants.LOCK_IMPORTED_APPS, "");
        Set<String> importedAppsSet = new HashSet<>();
        boolean hasImportedApps = !TextUtils.isEmpty(importedAppsStr);
        if (hasImportedApps) {
            for (String pkg : importedAppsStr.split(",")) {
                String trimmed = pkg.trim();
                if (!TextUtils.isEmpty(trimmed)) {
                    importedAppsSet.add(trimmed);
                }
            }
        }

        for (ResolveInfo resolveInfo : resolveInfos) {
            boolean isfaviterApp = isHasFaviterAppInfo(resolveInfo.activityInfo.packageName); //ob empfohlene zu sperrende App
            CommLockInfo commLockInfo = new CommLockInfo(resolveInfo.activityInfo.packageName, false, isfaviterApp); // Standardmäßige Schutzaktivierung muss noch hinzugefügt werden
            ApplicationInfo appInfo = mPackageManager.getApplicationInfo(commLockInfo.getPackageName(), PackageManager.GET_UNINSTALLED_PACKAGES);
            String appName = mPackageManager.getApplicationLabel(appInfo).toString();
            //einige Apps herausfiltern
            if (!commLockInfo.getPackageName().equals(AppConstants.APP_PACKAGE_NAME) && !commLockInfo.getPackageName().equals("com.android.settings")
                    && !commLockInfo.getPackageName().equals("com.google.android.googlequicksearchbox")) {
                // Sperrstatus: bei vorhandenem Import diesen verwenden, sonst Faviten-Standardliste
                if (hasImportedApps) {
                    commLockInfo.setLocked(importedAppsSet.contains(commLockInfo.getPackageName()));
                } else if (isfaviterApp) {
                    commLockInfo.setLocked(true);
                } else {
                    commLockInfo.setLocked(false);
                }
                commLockInfo.setAppName(appName);
                commLockInfo.setSetUnLock(false);

                list.add(commLockInfo);
            }
        }
        list = DataUtil.clearRepeatCommLockInfo(list);  //doppelte Einträge entfernen

        LitePal.saveAll(list);
    }

    /**
     * Prüfen ob empfohlene zu sperrende App
     */
    public boolean isHasFaviterAppInfo(String packageName) {
        List<FaviterInfo> infos = LitePal.where("packageName = ?", packageName).find(FaviterInfo.class);
        return infos.size() > 0;
    }

    /**
     * App-Status in Datenbank auf gesperrt setzen
     */
    public void lockCommApplication(String packageName) {
        updateLockStatus(packageName, true);
    }

    /**
     * App-Status in Datenbank auf entsperrt setzen
     */
    public void unlockCommApplication(String packageName) {
        updateLockStatus(packageName, false);
    }

    public void updateLockStatus(String packageName, boolean isLock) {
        ContentValues values = new ContentValues();
        values.put("isLocked", isLock);
        LitePal.updateAll(CommLockInfo.class, values, "packageName = ?", packageName);
    }


    /**
     * Prüfen ob App explizit nicht gesperrt werden soll
     */
    public boolean isSetUnLock(String packageName) {
        List<CommLockInfo> lockInfos = where("packageName = ?", packageName).find(CommLockInfo.class);
        for (CommLockInfo commLockInfo : lockInfos) {
            if (commLockInfo.isSetUnLock()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Prüfen ob Status gesperrt ist
     *
     * @param packageName
     * @return
     */
    public boolean isLockedPackageName(String packageName) {
        List<CommLockInfo> lockInfos = where("packageName = ?", packageName).find(CommLockInfo.class);
        for (CommLockInfo commLockInfo : lockInfos) {
            if (commLockInfo.isLocked()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Unscharfe Suche
     */
    public List<CommLockInfo> queryBlurryList(String appName) {
        List<CommLockInfo> infos = LitePal.where("appName like ?", "%" + appName + "%").find(CommLockInfo.class);
        return infos;
    }

    public void setIsUnLockThisApp(String packageName, boolean isSetUnLock) {
        ContentValues values = new ContentValues();
        values.put("isSetUnLock", isSetUnLock);
        LitePal.updateAll(CommLockInfo.class, values, "packageName = ?", packageName);
    }


    private Comparator commLockInfoComparator = new Comparator() {

        @Override
        public int compare(Object lhs, Object rhs) {
            CommLockInfo leftCommLockInfo = (CommLockInfo) lhs;
            CommLockInfo rightCommLockInfo = (CommLockInfo) rhs;

            if (leftCommLockInfo.isFaviterApp()
                    && !leftCommLockInfo.isLocked()
                    && !rightCommLockInfo.isFaviterApp()
                    && !rightCommLockInfo.isLocked()) {
                return -1;
            } else if (leftCommLockInfo.isFaviterApp()
                    && leftCommLockInfo.isLocked()
                    && !rightCommLockInfo.isFaviterApp()
                    && !rightCommLockInfo.isLocked()) {
                return -1;
            } else if (leftCommLockInfo.isFaviterApp()
                    && leftCommLockInfo.isLocked()
                    && rightCommLockInfo.isFaviterApp()
                    && !rightCommLockInfo.isLocked()) {
                if (leftCommLockInfo.getAppInfo() != null
                        && rightCommLockInfo.getAppInfo() != null)
                    return 1;
                else
                    return 0;
            } else if (!leftCommLockInfo.isFaviterApp()
                    && leftCommLockInfo.isLocked()
                    && !rightCommLockInfo.isFaviterApp()
                    && !rightCommLockInfo.isLocked()) {
                return -1;
            } else if (leftCommLockInfo.isFaviterApp()
                    && leftCommLockInfo.isLocked()
                    && !rightCommLockInfo.isFaviterApp()
                    && !rightCommLockInfo.isLocked()) {
                return -1;
            } else if (leftCommLockInfo.isFaviterApp()
                    && leftCommLockInfo.isLocked()
                    && rightCommLockInfo.isFaviterApp()
                    && !rightCommLockInfo.isLocked()) {
                if (leftCommLockInfo.getAppInfo() != null
                        && rightCommLockInfo.getAppInfo() != null)
                    return 1;
                else
                    return 0;
            } else if (!leftCommLockInfo.isFaviterApp()
                    && !leftCommLockInfo.isLocked()
                    && rightCommLockInfo.isFaviterApp()
                    && !rightCommLockInfo.isLocked()) {
                return 1;
            } else if (leftCommLockInfo.isFaviterApp()
                    && !leftCommLockInfo.isLocked()
                    && rightCommLockInfo.isFaviterApp()
                    && !rightCommLockInfo.isLocked()) {
                if (leftCommLockInfo.getAppInfo() != null
                        && rightCommLockInfo.getAppInfo() != null)
                    return 1;
                else
                    return 0;
            } else if (leftCommLockInfo.isFaviterApp()
                    && leftCommLockInfo.isLocked()
                    && rightCommLockInfo.isFaviterApp()
                    && !rightCommLockInfo.isLocked()) {
                if (leftCommLockInfo.getAppInfo() != null
                        && rightCommLockInfo.getAppInfo() != null)
                    return 1;
                else
                    return 0;
            } else if (!leftCommLockInfo.isFaviterApp()
                    && !leftCommLockInfo.isLocked()
                    && !rightCommLockInfo.isFaviterApp()
                    && rightCommLockInfo.isLocked()) {
                return 1;
            } else if (!leftCommLockInfo.isFaviterApp()
                    && !leftCommLockInfo.isLocked()
                    && rightCommLockInfo.isFaviterApp()
                    && rightCommLockInfo.isLocked()) {
                return 1;
            } else if (!leftCommLockInfo.isFaviterApp()
                    && leftCommLockInfo.isLocked()
                    && rightCommLockInfo.isFaviterApp()
                    && rightCommLockInfo.isLocked()) {
                return 1;
            } else if (!leftCommLockInfo.isFaviterApp()
                    && !leftCommLockInfo.isLocked()
                    && !rightCommLockInfo.isFaviterApp()
                    && !rightCommLockInfo.isLocked()) {
                if (leftCommLockInfo.getAppInfo() != null
                        && rightCommLockInfo.getAppInfo() != null)
                    return 1;
                else
                    return 0;
            } else if (leftCommLockInfo.isFaviterApp()
                    && leftCommLockInfo.isLocked()
                    && rightCommLockInfo.isFaviterApp()
                    && rightCommLockInfo.isLocked()) {
                if (leftCommLockInfo.getAppInfo() != null
                        && rightCommLockInfo.getAppInfo() != null)
                    return 1;
                else
                    return 0;
            } else if (!leftCommLockInfo.isFaviterApp()
                    && !leftCommLockInfo.isLocked()
                    && rightCommLockInfo.isFaviterApp()
                    && rightCommLockInfo.isLocked()) {
                return 1;
            } else if (!leftCommLockInfo.isFaviterApp()
                    && leftCommLockInfo.isLocked()
                    && !rightCommLockInfo.isFaviterApp()
                    && !rightCommLockInfo.isLocked()) {
                return -1;
            }
            return 0;
        }
    };

}

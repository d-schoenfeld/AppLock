package com.lzx.lock.mvp.p;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.lzx.lock.base.AppConstants;
import com.lzx.lock.bean.CommLockInfo;
import com.lzx.lock.db.DbManager;
import com.lzx.lock.mvp.contract.MainContract;
import com.lzx.lock.utils.SpUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by xian on 2017/2/17.
 */

public class MainPresenter implements MainContract.Presenter {

    private MainContract.View mView;
    private PackageManager mPackageManager;
    private Context mContext;
    private CompositeDisposable mDisposables;

    public MainPresenter(MainContract.View view, Context mContext) {
        mView = view;
        this.mContext = mContext;
        mPackageManager = mContext.getPackageManager();
        mDisposables = new CompositeDisposable();
    }

    @Override
    public void loadAppInfo(Context context, final boolean isSort) {
        mDisposables.add(
                Observable.fromCallable(new Callable<List<CommLockInfo>>() {
                    @Override
                    public List<CommLockInfo> call() throws Exception {
                        return loadAppInfoSync(isSort);
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<CommLockInfo>>() {
                    @Override
                    public void accept(List<CommLockInfo> commLockInfos) throws Exception {
                        mView.loadAppInfoSuccess(commLockInfos);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                })
        );
    }

    @Override
    public void loadLockAppInfo(Context context) {
        mDisposables.add(
                Observable.fromCallable(new Callable<List<CommLockInfo>>() {
                    @Override
                    public List<CommLockInfo> call() throws Exception {
                        return loadLockAppInfoSync();
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<CommLockInfo>>() {
                    @Override
                    public void accept(List<CommLockInfo> commLockInfos) throws Exception {
                        mView.loadAppInfoSuccess(commLockInfos);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                })
        );
    }

    private List<CommLockInfo> loadAppInfoSync(boolean isSort) {
        List<CommLockInfo> commLockInfos = DbManager.get().queryInfoList();
        Iterator<CommLockInfo> infoIterator = commLockInfos.iterator();
        int favoriteNum = 0;
        int sysAppNum = 0;
        int userAppNum = 0;

        while (infoIterator.hasNext()) {
            CommLockInfo info = infoIterator.next();
            try {
                ApplicationInfo appInfo = mPackageManager.getApplicationInfo(info.getPackageName(), PackageManager.GET_UNINSTALLED_PACKAGES);
                if (appInfo == null || mPackageManager.getApplicationIcon(appInfo) == null) {
                    infoIterator.remove(); //fehlerhafte Apps entfernen
                    continue;
                } else {
                    info.setAppInfo(appInfo); //ApplicationInfo der Liste zuweisen
                    if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) { //prüfen ob System-App ApplicationInfo#isSystemApp()
                        info.setSysApp(true);
                        info.setTopTitle("System-Apps");
                    } else {
                        info.setSysApp(false);
                        info.setTopTitle("Benutzer-Apps");
                    }
                }
                //Gesamtzahl der gesperrten Apps ermitteln
                if (info.isLocked()) {
                    favoriteNum++;
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                infoIterator.remove();
            }
        }
        SpUtil.getInstance().putInt(AppConstants.LOCK_FAVITER_NUM, favoriteNum);

        if (isSort) {
            List<CommLockInfo> sysList = new ArrayList<>();
            List<CommLockInfo> userList = new ArrayList<>();
            for (CommLockInfo info : commLockInfos) {
                if (info.isSysApp()) {
                    sysList.add(info);
                    sysAppNum++;
                } else {
                    userList.add(info);
                    userAppNum++;
                }
            }
            SpUtil.getInstance().putInt(AppConstants.LOCK_SYS_APP_NUM, sysAppNum);
            SpUtil.getInstance().putInt(AppConstants.LOCK_USER_APP_NUM, userAppNum);
            commLockInfos.clear();
            commLockInfos.addAll(sysList);
            commLockInfos.addAll(userList);
        }
        return commLockInfos;
    }

    private List<CommLockInfo> loadLockAppInfoSync() {
        List<CommLockInfo> commLockInfos = DbManager.get().queryInfoList();
        Iterator<CommLockInfo> infoIterator = commLockInfos.iterator();

        while (infoIterator.hasNext()) {
            CommLockInfo info = infoIterator.next();
            try {
                ApplicationInfo appInfo = mPackageManager.getApplicationInfo(info.getPackageName(), PackageManager.GET_UNINSTALLED_PACKAGES);
                if (appInfo == null || mPackageManager.getApplicationIcon(appInfo) == null) {
                    infoIterator.remove(); //fehlerhafte Apps entfernen
                    continue;
                } else {
                    info.setAppInfo(appInfo); //ApplicationInfo der Liste zuweisen
                    if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) { //prüfen ob System-App ApplicationInfo#isSystemApp()
                        info.setSysApp(true);
                        info.setTopTitle("System-Apps");
                    } else {
                        info.setSysApp(false);
                        info.setTopTitle("Benutzer-Apps");
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                infoIterator.remove();
            }
        }

        List<CommLockInfo> list = new ArrayList<>();
        for (CommLockInfo info : commLockInfos) {
            if (info.isLocked()) {
                list.add(info);
            }
        }
        return list;
    }

    @Override
    public void onDestroy() {
        mDisposables.clear();
    }
}

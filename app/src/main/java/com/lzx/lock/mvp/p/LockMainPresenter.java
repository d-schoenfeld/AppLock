package com.lzx.lock.mvp.p;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.lzx.lock.R;
import com.lzx.lock.base.AppConstants;
import com.lzx.lock.bean.CommLockInfo;
import com.lzx.lock.db.DbManager;
import com.lzx.lock.mvp.contract.LockMainContract;
import com.lzx.lock.utils.SpUtil;

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

public class LockMainPresenter implements LockMainContract.Presenter {

    private LockMainContract.View mView;
    private PackageManager mPackageManager;
    private Context mContext;
    private CompositeDisposable mDisposables;

    public LockMainPresenter(LockMainContract.View view, Context mContext) {
        mView = view;
        this.mContext = mContext;
        mPackageManager = mContext.getPackageManager();
        mDisposables = new CompositeDisposable();
    }

    /**
     * Alle Apps laden
     */
    @Override
    public void loadAppInfo(Context context) {
        mDisposables.add(
                Observable.fromCallable(new Callable<List<CommLockInfo>>() {
                    @Override
                    public List<CommLockInfo> call() throws Exception {
                        return loadAppInfoSync();
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
    public void searchAppInfo(final String search, final ISearchResultListener listener) {
        mDisposables.add(
                Observable.fromCallable(new Callable<List<CommLockInfo>>() {
                    @Override
                    public List<CommLockInfo> call() throws Exception {
                        return searchAppInfoSync(search);
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<CommLockInfo>>() {
                    @Override
                    public void accept(List<CommLockInfo> commLockInfos) throws Exception {
                        listener.onSearchResult(commLockInfos);
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
    public void onDestroy() {
        mDisposables.clear();
    }

    private List<CommLockInfo> loadAppInfoSync() {
        List<CommLockInfo> commLockInfos = DbManager.get().queryInfoList();
        Iterator<CommLockInfo> infoIterator = commLockInfos.iterator();
        int favoriteNum = 0;

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
                        info.setTopTitle(mContext.getString(R.string.tab_system_apps));
                    } else {
                        info.setSysApp(false);
                        info.setTopTitle(mContext.getString(R.string.tab_user_apps));
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
        return commLockInfos;
    }

    private List<CommLockInfo> searchAppInfoSync(String search) {
        List<CommLockInfo> commLockInfos = DbManager.get().queryInfoList();
        Iterator<CommLockInfo> infoIterator = commLockInfos.iterator();
        while (infoIterator.hasNext()) {
            CommLockInfo info = infoIterator.next();
            String appName = info.getAppName();
            if (appName == null || !appName.toLowerCase().contains(search.toLowerCase())) {
                infoIterator.remove();
                continue;
            }
            try {
                ApplicationInfo appInfo = mPackageManager.getApplicationInfo(info.getPackageName(), PackageManager.GET_UNINSTALLED_PACKAGES);
                if (appInfo == null || mPackageManager.getApplicationIcon(appInfo) == null) {
                    infoIterator.remove(); //fehlerhafte Apps entfernen
                    continue;
                } else {
                    info.setAppInfo(appInfo); //ApplicationInfo der Liste zuweisen
                    if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) { //prüfen ob System-App ApplicationInfo#isSystemApp()
                        info.setSysApp(true);
                        info.setTopTitle(mContext.getString(R.string.tab_system_apps));
                    } else {
                        info.setSysApp(false);
                        info.setTopTitle(mContext.getString(R.string.tab_user_apps));
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                infoIterator.remove();
            }
        }
        return commLockInfos;
    }

    public interface ISearchResultListener {
        void onSearchResult(List<CommLockInfo> commLockInfos);
    }
}

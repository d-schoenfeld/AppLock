package com.lzx.lock.mvp.p;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;

import com.lzx.lock.base.AppConstants;
import com.lzx.lock.bean.CommLockInfo;
import com.lzx.lock.db.CommLockInfoManager;
import com.lzx.lock.mvp.contract.LockMainContract;
import com.lzx.lock.utils.SpUtil;

import java.util.Iterator;
import java.util.List;

/**
 * Created by xian on 2017/2/17.
 */

public class LockMainPresenter implements LockMainContract.Presenter {

    private LockMainContract.View mView;
    private PackageManager mPackageManager;
    private CommLockInfoManager mLockInfoManager;
    private Context mContext;
    private LoadAppInfo mLoadAppInfo;

    public LockMainPresenter(LockMainContract.View view, Context mContext) {
        mView = view;
        this.mContext = mContext;
        mPackageManager = mContext.getPackageManager();
        mLockInfoManager = new CommLockInfoManager(mContext);
    }

    /**
     * Alle Apps laden
     */
    @Override
    public void loadAppInfo(Context context) {
        mLoadAppInfo = new LoadAppInfo();
        mLoadAppInfo.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void searchAppInfo(String search, ISearchResultListener listener) {
        new SearchInfoAsyncTask(listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, search);
    }


    @Override
    public void onDestroy() {
        if (mLoadAppInfo != null && mLoadAppInfo.getStatus() != AsyncTask.Status.FINISHED) {
            mLoadAppInfo.cancel(true);
        }
    }

    private class LoadAppInfo extends AsyncTask<Void, String, List<CommLockInfo>> {

        @Override
        protected List<CommLockInfo> doInBackground(Void... params) {
            List<CommLockInfo> commLockInfos = mLockInfoManager.getAllCommLockInfos();
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
            return commLockInfos;
        }

        @Override
        protected void onPostExecute(List<CommLockInfo> commLockInfos) {
            super.onPostExecute(commLockInfos);
            mView.loadAppInfoSuccess(commLockInfos);
        }
    }

    private class SearchInfoAsyncTask extends AsyncTask<String, Void, List<CommLockInfo>> {
        private ISearchResultListener mSearchResultListener;

        public SearchInfoAsyncTask(ISearchResultListener searchResultListener) {
            mSearchResultListener = searchResultListener;
        }

        @Override
        protected List<CommLockInfo> doInBackground(String... params) {
            List<CommLockInfo> commLockInfos = mLockInfoManager.queryBlurryList(params[0]);
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
            return commLockInfos;
        }

        @Override
        protected void onPostExecute(List<CommLockInfo> commLockInfos) {
            super.onPostExecute(commLockInfos);
            mSearchResultListener.onSearchResult(commLockInfos);
        }
    }

    public interface ISearchResultListener {
        void onSearchResult(List<CommLockInfo> commLockInfos);
    }
}

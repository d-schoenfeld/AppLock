package com.lzx.lock.module.splash;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ImageView;
import android.widget.Toast;

import com.lzx.lock.R;
import com.lzx.lock.module.lock.GestureSelfUnlockActivity;
import com.lzx.lock.base.BaseActivity;
import com.lzx.lock.base.AppConstants;
import com.lzx.lock.bean.CommLockInfo;
import com.lzx.lock.db.DbManager;
import com.lzx.lock.helper.LoadAppHelper;
import com.lzx.lock.module.pwd.CreatePwdActivity;
import com.lzx.lock.service.LockService;
import com.lzx.lock.utils.AppUtils;
import com.lzx.lock.utils.LockUtil;
import com.lzx.lock.utils.SpUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by xian on 2017/2/17.
 */

public class SplashActivity extends BaseActivity {

    private ImageView mImgSplash;
    private ObjectAnimator animator;
    private static final int RC_PERMISSION_SETUP = 1;
    private CompositeDisposable mDisposables;

    @Override
    public int getLayoutId() {
        return R.layout.activity_splash;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        AppUtils.hideStatusBar(getWindow(), true);
        mImgSplash = (ImageView) findViewById(R.id.img_splash);
    }

    @Override
    protected void initData() {
        mDisposables = new CompositeDisposable();
        if (SpUtil.getInstance().getBoolean(AppConstants.LOCK_STATE, false)) {
            Intent serviceIntent = new Intent(this, LockService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
        animator = ObjectAnimator.ofFloat(mImgSplash, "alpha", 0.5f, 1);
        animator.setDuration(1500);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                handleAfterSplash();
            }
        });
        initAppData();
    }

    /**
     * App-Daten mit RxJava initialisieren
     */
    private void initAppData() {
        final boolean isFirstTime = !SpUtil.getInstance().getBoolean(AppConstants.LOCK_IS_INIT_DB, false);

        mDisposables.add(
                LoadAppHelper.loadAllLockAppInfoAsync(this)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .filter(new Predicate<List<CommLockInfo>>() {
                            @Override
                            public boolean test(List<CommLockInfo> lockAppInfos) throws Exception {
                                if (isFirstTime) {
                                    SpUtil.getInstance().putBoolean(AppConstants.LOCK_IS_INIT_DB, true);
                                    mDisposables.add(
                                            DbManager.get().saveLockAppInfoListAsync(lockAppInfos)
                                                    .subscribeOn(Schedulers.newThread())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe(new Consumer<Boolean>() {
                                                        @Override
                                                        public void accept(Boolean aBoolean) throws Exception {
                                                            animator.start();
                                                        }
                                                    }, new Consumer<Throwable>() {
                                                        @Override
                                                        public void accept(Throwable throwable) throws Exception {
                                                            animator.start();
                                                        }
                                                    })
                                    );
                                    return false;
                                } else {
                                    return true;
                                }
                            }
                        })
                        .observeOn(Schedulers.newThread())
                        .map(new Function<List<CommLockInfo>, List<CommLockInfo>>() {
                            @Override
                            public List<CommLockInfo> apply(List<CommLockInfo> appList) throws Exception {
                                //Datenbankeinträge mit aktuellen App-Daten vergleichen
                                List<CommLockInfo> dbList = DbManager.get().queryInfoList();
                                if (appList.size() > dbList.size()) { //wenn neue App installiert wurde
                                    List<CommLockInfo> resultList = new ArrayList<>();
                                    HashMap<String, CommLockInfo> hashMap = new HashMap<>();
                                    for (CommLockInfo info : dbList) {
                                        hashMap.put(info.getPackageName(), info);
                                    }
                                    for (CommLockInfo info : appList) {
                                        if (!hashMap.containsKey(info.getPackageName())) {
                                            resultList.add(info);
                                        }
                                    }
                                    //neue Apps in Datenbank einfügen
                                    if (resultList.size() != 0) {
                                        DbManager.get().saveInfoList(resultList);
                                    }
                                } else if (appList.size() < dbList.size()) { //wenn App deinstalliert wurde
                                    List<CommLockInfo> resultList = new ArrayList<>();
                                    HashMap<String, CommLockInfo> hashMap = new HashMap<>();
                                    for (CommLockInfo info : appList) {
                                        hashMap.put(info.getPackageName(), info);
                                    }
                                    for (CommLockInfo info : dbList) {
                                        if (!hashMap.containsKey(info.getPackageName())) {
                                            resultList.add(info);
                                        }
                                    }
                                    //deinstallierte Apps aus Datenbank löschen
                                    if (resultList.size() != 0) {
                                        DbManager.get().deleteInfoByList(resultList);
                                    }
                                }
                                return DbManager.get().queryInfoList();
                            }
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<List<CommLockInfo>>() {
                            @Override
                            public void accept(List<CommLockInfo> lockAppInfos) throws Exception {
                                if (lockAppInfos.size() != 0) {
                                    animator.start();
                                } else {
                                    Toast.makeText(SplashActivity.this, "Fehler bei der Datenverarbeitung", Toast.LENGTH_SHORT).show();
                                    animator.start();
                                }
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                Toast.makeText(SplashActivity.this, "Fehler bei der Datenverarbeitung", Toast.LENGTH_SHORT).show();
                                animator.start();
                            }
                        })
        );
    }

    /**
     * Nach der Splash-Animation: Berechtigungen prüfen und ggf. Einrichtungsassistenten starten.
     * Der Assistent wird auch bei späteren Starts angezeigt, falls Berechtigungen fehlen.
     */
    private void handleAfterSplash() {
        if (!allRequiredPermissionsGranted()) {
            startActivityForResult(
                    new Intent(SplashActivity.this, PermissionSetupActivity.class),
                    RC_PERMISSION_SETUP);
        } else {
            proceedAfterPermissions();
        }
    }

    /**
     * Prüft ob alle für den App-Start erforderlichen Berechtigungen erteilt wurden.
     */
    private boolean allRequiredPermissionsGranted() {
        return LockUtil.isStatAccessPermissionSet(this)
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                    || Settings.canDrawOverlays(this));
    }

    /**
     * Navigiert nach erfolgreich erteilten Berechtigungen zur nächsten Ansicht.
     */
    private void proceedAfterPermissions() {
        boolean isFirstLock = SpUtil.getInstance().getBoolean(AppConstants.LOCK_IS_FIRST_LOCK, true);
        if (isFirstLock) {
            gotoCreatePwdActivity();
        } else {
            Intent intent = new Intent(SplashActivity.this, GestureSelfUnlockActivity.class);
            intent.putExtra(AppConstants.LOCK_PACKAGE_NAME, AppConstants.APP_PACKAGE_NAME);
            intent.putExtra(AppConstants.LOCK_FROM, AppConstants.LOCK_FROM_LOCK_MAIN_ACITVITY);
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PERMISSION_SETUP) {
            if (resultCode == RESULT_OK) {
                proceedAfterPermissions();
            } else {
                // Erforderliche Berechtigungen wurden nicht erteilt – App kann nicht gestartet werden.
                finish();
            }
        }
    }

    private void gotoCreatePwdActivity() {
        Intent intent = new Intent(SplashActivity.this, CreatePwdActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void initAction() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDisposables != null) {
            mDisposables.clear();
        }
        animator = null;
    }
}

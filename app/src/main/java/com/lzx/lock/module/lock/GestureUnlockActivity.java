package com.lzx.lock.module.lock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lzx.lock.R;
import com.lzx.lock.base.BaseActivity;
import com.lzx.lock.base.AppConstants;
import com.lzx.lock.db.CommLockInfoManager;
import com.lzx.lock.module.main.MainActivity;
import com.lzx.lock.service.LockService;
import com.lzx.lock.utils.LockPatternUtils;
import com.lzx.lock.utils.LockUtil;
import com.lzx.lock.utils.SpUtil;
import com.lzx.lock.utils.StatusBarUtil;
import com.lzx.lock.widget.LockPatternView;
import com.lzx.lock.widget.LockPatternViewPattern;
import com.lzx.lock.widget.UnLockMenuPopWindow;

import java.util.List;

/**
 * Created by xian on 2017/2/17.
 */

public class GestureUnlockActivity extends BaseActivity implements View.OnClickListener {

    private ImageView mIconMore;
    private LockPatternView mLockPatternView;
    private ImageView mUnLockIcon, mBgLayout, mAppLogo;
    private TextView mUnLockText, mUnlockFailTip, mAppLabel;
    private RelativeLayout mUnLockLayout;

    private PackageManager packageManager;
    private String pkgName; //Paketname der zu entsperrenden App
    private String actionFrom;//Aktion bei Zurück-Taste
    private LockPatternUtils mLockPatternUtils;
    private int mFailedPatternAttemptsSinceLastTimeout = 0;
    private CommLockInfoManager mLockInfoManager;
    private UnLockMenuPopWindow mPopWindow;
    private LockPatternViewPattern mPatternViewPattern;
    private GestureUnlockReceiver mGestureUnlockReceiver;
    private ApplicationInfo appInfo;
    public static final String FINISH_UNLOCK_THIS_APP = "finish_unlock_this_app";

    private Drawable iconDrawable;
    private String appLabel;
    @Override
    public int getLayoutId() {
        return R.layout.activity_gesture_unlock;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        StatusBarUtil.setTransparent(this); //Statusleiste transparent
        mUnLockLayout = (RelativeLayout) findViewById(R.id.unlock_layout);
        mIconMore = (ImageView) findViewById(R.id.btn_more);
        mLockPatternView = (LockPatternView) findViewById(R.id.unlock_lock_view);
        mUnLockIcon = (ImageView) findViewById(R.id.unlock_icon);
        mBgLayout = (ImageView) findViewById(R.id.bg_layout);
        mUnLockText = (TextView) findViewById(R.id.unlock_text);
        mUnlockFailTip = (TextView) findViewById(R.id.unlock_fail_tip);

        mAppLogo = (ImageView) findViewById(R.id.app_logo);
        mAppLabel = (TextView) findViewById(R.id.app_label);


    }

    @Override
    protected void initData() {
        //Paketname der zu entsperrenden App abrufen
        pkgName = getIntent().getStringExtra(AppConstants.LOCK_PACKAGE_NAME);
        //Aktion bei Zurück-Taste abrufen
        actionFrom = getIntent().getStringExtra(AppConstants.LOCK_FROM);
        //Initialisieren
        packageManager = getPackageManager();

        mLockInfoManager = new CommLockInfoManager(this);
        mPopWindow = new UnLockMenuPopWindow(this, pkgName, true);

        initLayoutBackground();
        initLockPatternView();


        mGestureUnlockReceiver = new GestureUnlockReceiver();
        IntentFilter filter = new IntentFilter();
      //  filter.addAction(UnLockMenuPopWindow.UPDATE_LOCK_VIEW);
        filter.addAction(FINISH_UNLOCK_THIS_APP);
        registerReceiver(mGestureUnlockReceiver, filter);

    }

    /**
     * App-Icon und Hintergrund zuweisen
     */
    private void initLayoutBackground() {
        try {
            appInfo = packageManager.getApplicationInfo(pkgName, PackageManager.GET_UNINSTALLED_PACKAGES);
            if (appInfo != null) {
                iconDrawable = packageManager.getApplicationIcon(appInfo);
                appLabel = packageManager.getApplicationLabel(appInfo).toString();
                mUnLockIcon.setImageDrawable(iconDrawable);
                mUnLockText.setText(appLabel);
                mUnlockFailTip.setText(getString(R.string.password_gestrue_tips));
                final Drawable icon = packageManager.getApplicationIcon(appInfo);
                mUnLockLayout.setBackgroundDrawable(icon);
                mUnLockLayout.getViewTreeObserver().addOnPreDrawListener(
                        new ViewTreeObserver.OnPreDrawListener() {
                            @Override
                            public boolean onPreDraw() {
                                mUnLockLayout.getViewTreeObserver().removeOnPreDrawListener(this);
                                mUnLockLayout.buildDrawingCache();
                                Bitmap bmp = LockUtil.drawableToBitmap(icon, mUnLockLayout);
                                LockUtil.blur(GestureUnlockActivity.this, LockUtil.big(bmp), mUnLockLayout);  //Gaußscher Weichzeichner
                                return true;
                            }
                        });
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Entsperr-Widget initialisieren
     */
    private void initLockPatternView() {
        mLockPatternView.setLineColorRight(0x80ffffff);
        mLockPatternUtils = new LockPatternUtils(this);
        mPatternViewPattern = new LockPatternViewPattern(mLockPatternView);
        mPatternViewPattern.setPatternListener(new LockPatternViewPattern.onPatternListener() {
            @Override
            public void onPatternDetected(List<LockPatternView.Cell> pattern) {
                if (mLockPatternUtils.checkPattern(pattern)) { //Entsperrung erfolgreich, Datenbankstatus aktualisieren
                    mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);
                    if (actionFrom.equals(AppConstants.LOCK_FROM_LOCK_MAIN_ACITVITY)) {
                        startActivity(new Intent(GestureUnlockActivity.this, MainActivity.class));
                        finish();
                    } else {
                        long unlockTime = System.currentTimeMillis();
                        SpUtil.getInstance().putLong(AppConstants.LOCK_CURR_MILLISENCONS, unlockTime); //Entsperrzeit speichern
                        SpUtil.getInstance().putString(AppConstants.LOCK_LAST_LOAD_PKG_NAME, pkgName);//Entsperr-Paketnamen speichern
                        LockService.sLockCurrMilliseconds = unlockTime; //Statische Variable aktualisieren
                        LockService.sLastLoadPkgName = pkgName; //Statische Variable aktualisieren

                        //Zeitpunkt der letzten Entsperrung an den App-Sperrdienst senden
                        Intent intent = new Intent(LockService.UNLOCK_ACTION);
                        intent.putExtra(LockService.LOCK_SERVICE_LASTTIME, System.currentTimeMillis());
                        intent.putExtra(LockService.LOCK_SERVICE_LASTAPP, pkgName);
                        sendBroadcast(intent);

                        mLockInfoManager.unlockCommApplication(pkgName);
                        finish();
                    }
                } else {
                    mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    if (pattern.size() >= LockPatternUtils.MIN_PATTERN_REGISTER_FAIL) {
                        mFailedPatternAttemptsSinceLastTimeout++;
                        int retry = LockPatternUtils.FAILED_ATTEMPTS_BEFORE_TIMEOUT - mFailedPatternAttemptsSinceLastTimeout;
                        if (retry >= 0) {
                            String format = getResources().getString(R.string.password_error_count);
//                            String str = String.format(format, retry);
                            // mUnlockFailTip.setText(str);
//                            ToastUtil.showShort(str);
                        }
                    } else {
//                        ToastUtil.showShort(getString(R.string.password_short));
                    }
                    if (mFailedPatternAttemptsSinceLastTimeout >= 3) { //Fehlversuche > 3
                        mLockPatternView.postDelayed(mClearPatternRunnable, 500);
                    }
                    if (mFailedPatternAttemptsSinceLastTimeout >= LockPatternUtils.FAILED_ATTEMPTS_BEFORE_TIMEOUT) { //Fehlversuche >= Maximum (Benutzer wird blockiert)
                        mLockPatternView.postDelayed(mClearPatternRunnable, 500);
                    } else {
                        mLockPatternView.postDelayed(mClearPatternRunnable, 500);
                    }
                }
            }
        });
        mLockPatternView.setOnPatternListener(mPatternViewPattern);
        mLockPatternView.setTactileFeedbackEnabled(true);
    }

    private Runnable mClearPatternRunnable = new Runnable() {
        public void run() {
            mLockPatternView.clearPattern();
        }
    };

    @Override
    public void onBackPressed() {
        if (actionFrom.equals(AppConstants.LOCK_FROM_FINISH)) {
            LockUtil.goHome(this);
        } else if (actionFrom.equals(AppConstants.LOCK_FROM_LOCK_MAIN_ACITVITY)) {
            finish();
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
    }
    
    @Override
    protected void initAction() {
        mIconMore.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_more:
                mPopWindow.showAsDropDown(mIconMore);
                break;
        }
    }

    private class GestureUnlockReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
//            if (action.equals(UnLockMenuPopWindow.UPDATE_LOCK_VIEW)) {
//                mLockPatternView.initRes();
//            } else
            if (action.equals(FINISH_UNLOCK_THIS_APP)) {
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mGestureUnlockReceiver);
    }
}

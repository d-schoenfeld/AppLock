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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lzx.lock.R;
import com.lzx.lock.base.BaseActivity;
import com.lzx.lock.base.AppConstants;
import com.lzx.lock.db.CommLockInfoManager;
import com.lzx.lock.module.main.MainActivity;
import com.lzx.lock.service.Camera2Manager;
import com.lzx.lock.service.LockService;
import com.lzx.lock.utils.LockPatternUtils;
import com.lzx.lock.utils.LockUtil;
import com.lzx.lock.utils.PinUtils;
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

    /** Gibt an, ob der Sperr-Bildschirm gerade angezeigt wird (für LockService-Guard). */
    public static volatile boolean isShowing = false;

    private ImageView mIconMore;
    private LockPatternView mLockPatternView;
    private ImageView mUnLockIcon, mBgLayout, mAppLogo;
    private TextView mUnLockText, mUnlockFailTip, mAppLabel;
    private RelativeLayout mUnLockLayout;

    // PIN-Entsperrung
    private LinearLayout mPinUnlockSection;
    private EditText mEtPinUnlock;
    private TextView mBtnPinUnlock;

    private PackageManager packageManager;
    private String pkgName; //Paketname der zu entsperrenden App
    private String actionFrom;//Aktion bei Zurück-Taste
    private LockPatternUtils mLockPatternUtils;
    private int mFailedPatternAttemptsSinceLastTimeout = 0;
    private int mFailedPicAttempts = 0;
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

        mPinUnlockSection = (LinearLayout) findViewById(R.id.pin_unlock_section);
        mEtPinUnlock = (EditText) findViewById(R.id.et_pin_unlock);
        mBtnPinUnlock = (TextView) findViewById(R.id.btn_pin_unlock);
    }

    @Override
    protected void initData() {
        //Paketname der zu entsperrenden App abrufen
        pkgName = getIntent().getStringExtra(AppConstants.LOCK_PACKAGE_NAME);
        //Aktion bei Zurück-Taste abrufen; Standard: LOCK_FROM_FINISH (wenn vom Dienst gestartet)
        actionFrom = getIntent().getStringExtra(AppConstants.LOCK_FROM);
        if (actionFrom == null) actionFrom = AppConstants.LOCK_FROM_FINISH;
        //Initialisieren
        packageManager = getPackageManager();
        mFailedPicAttempts = 0;

        mLockInfoManager = new CommLockInfoManager(this);
        mPopWindow = new UnLockMenuPopWindow(this, pkgName, true);

        initLayoutBackground();
        initLockPatternView();
        initPinUnlockView();
        initUnlockViews();

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
                    handleUnlockSuccess();
                } else {
                    mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    takePhotoIfEnabled();
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

    /**
     * PIN-Entsperrung initialisieren
     */
    private void initPinUnlockView() {
        mBtnPinUnlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pin = mEtPinUnlock.getText().toString().trim();
                if (pin.length() < PinUtils.MIN_PIN_LENGTH) {
                    mUnlockFailTip.setText(getString(R.string.pin_too_short));
                    return;
                }
                if (PinUtils.checkPin(pin)) {
                    mEtPinUnlock.setText("");
                    handleUnlockSuccess();
                } else {
                    mEtPinUnlock.setText("");
                    mUnlockFailTip.setText(getString(R.string.pin_wrong));
                    takePhotoIfEnabled();
                }
            }
        });
    }

    /**
     * Entsperrungsmethode bestimmen und passende UI anzeigen
     */
    private void initUnlockViews() {
        if (PinUtils.isPinMethod()) {
            mLockPatternView.setVisibility(View.GONE);
            mPinUnlockSection.setVisibility(View.VISIBLE);
            mUnlockFailTip.setText(getString(R.string.pin_enter_to_unlock));
        } else {
            mLockPatternView.setVisibility(View.VISIBLE);
            mPinUnlockSection.setVisibility(View.GONE);
        }
    }

    /**
     * Entsperrung erfolgreich – Zustand aktualisieren und Activity beenden
     */
    private void handleUnlockSuccess() {
        if (AppConstants.LOCK_FROM_LOCK_MAIN_ACITVITY.equals(actionFrom)) {
            Intent mainIntent = new Intent(GestureUnlockActivity.this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mainIntent);
            finish();
        } else {
            long unlockTime = System.currentTimeMillis();
            SpUtil.getInstance().putLong(AppConstants.LOCK_CURR_MILLISENCONS, unlockTime);
            SpUtil.getInstance().putString(AppConstants.LOCK_LAST_LOAD_PKG_NAME, pkgName);
            LockService.sLockCurrMilliseconds = unlockTime;
            LockService.sLastLoadPkgName = pkgName;

            Intent intent = new Intent(LockService.UNLOCK_ACTION);
            intent.putExtra(LockService.LOCK_SERVICE_LASTTIME, unlockTime);
            intent.putExtra(LockService.LOCK_SERVICE_LASTAPP, pkgName);
            sendBroadcast(intent);

            mLockInfoManager.unlockCommApplication(pkgName);
            finish();
        }
    }

    /**
     * Nimmt ein Foto auf, wenn die Einstellung "Bei Fehleingabe fotografieren" aktiviert ist
     * und die konfigurierte Anzahl von Fehlversuchen erreicht wurde.
     */
    private void takePhotoIfEnabled() {
        if (SpUtil.getInstance().getBoolean(AppConstants.LOCK_AUTO_RECORD_PIC, false)) {
            mFailedPicAttempts++;
            int threshold = (int) SpUtil.getInstance().getInt(AppConstants.LOCK_AUTO_RECORD_PIC_ATTEMPT, 1);
            if (mFailedPicAttempts >= threshold) {
                mFailedPicAttempts = 0;
                new Camera2Manager(this).capturePhoto();
            }
        }
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
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mainIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isShowing = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isShowing = false;
    }
    
    @Override
    protected void initAction() {
        mIconMore.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_more:
                onBackPressed();
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

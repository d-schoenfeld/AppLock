package com.lzx.lock.module.lock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lzx.lock.R;
import com.lzx.lock.base.AppConstants;
import com.lzx.lock.db.CommLockInfoManager;
import com.lzx.lock.service.Camera2Manager;
import com.lzx.lock.service.LockService;
import com.lzx.lock.utils.LockPatternUtils;
import com.lzx.lock.utils.LockUtil;
import com.lzx.lock.utils.PinUtils;
import com.lzx.lock.utils.SpUtil;
import com.lzx.lock.utils.ToastUtil;
import com.lzx.lock.widget.LockPatternView;
import com.lzx.lock.widget.LockPatternViewPattern;

import java.util.List;

/**
 * Schwebende Entsperrungsansicht als Alternative zur GestureUnlockActivity.
 * Wird über WindowManager als überlagerndes Fenster angezeigt.
 */
public class UnlockView extends FrameLayout {

    private int mFailedPatternAttemptsSinceLastTimeout = 0;
    private int mFailedPicAttempts = 0;

    private WindowManager.LayoutParams mLayoutParams;
    private WindowManager mWindowManager;
    private Context mContext;
    private View mUnLockView;

    private RelativeLayout mUnLockLayout;
    private ImageView mUnLockIcon, mAppLogo;
    private TextView mUnLockText, mUnlockFailTip, mAppLabel;

    // Muster-Entsperrung
    private LockPatternView mPatternView;
    private LockPatternUtils mPatternUtils;
    private LockPatternViewPattern mPatternViewPattern;

    // PIN-Entsperrung
    private LinearLayout mPinUnlockSection;
    private EditText mEtPinUnlock;
    private TextView mBtnPinUnlock;

    private CommLockInfoManager mLockInfoManager;
    private PackageManager mPackageManager;

    private String mPackageName;

    /** Broadcast-Aktion zum Schließen der Entsperrungsansicht */
    public static final String FINISH_UNLOCK_THIS_APP = "finish_unlock_this_app";

    private final BroadcastReceiver mFinishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (FINISH_UNLOCK_THIS_APP.equals(intent.getAction())) {
                closeUnLockView();
            }
        }
    };

    private static final int MSG_GO_HOME = 200;

    public UnlockView(@NonNull Context context) {
        super(context);
        init();
    }

    private void init() {
        mContext = getContext();
        mPackageManager = mContext.getPackageManager();
        mLockInfoManager = new CommLockInfoManager(mContext);

        mUnLockView = LayoutInflater.from(mContext).inflate(R.layout.layout_unlock_view, this);
        mUnLockLayout = (RelativeLayout) mUnLockView.findViewById(R.id.unlock_layout);
        mUnLockIcon = (ImageView) mUnLockView.findViewById(R.id.unlock_icon);
        mAppLogo = (ImageView) mUnLockView.findViewById(R.id.app_logo);
        mUnLockText = (TextView) mUnLockView.findViewById(R.id.unlock_text);
        mUnlockFailTip = (TextView) mUnLockView.findViewById(R.id.unlock_fail_tip);
        mAppLabel = (TextView) mUnLockView.findViewById(R.id.app_label);

        mPatternView = (LockPatternView) mUnLockView.findViewById(R.id.unlock_lock_view);
        mPinUnlockSection = (LinearLayout) mUnLockView.findViewById(R.id.pin_unlock_section);
        mEtPinUnlock = (EditText) mUnLockView.findViewById(R.id.et_pin_unlock);
        mBtnPinUnlock = (TextView) mUnLockView.findViewById(R.id.btn_pin_unlock);

        // Schwebendes Fenster erstellen
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        int windowType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            windowType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            windowType = WindowManager.LayoutParams.TYPE_PHONE;
        }
        mLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                windowType,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSPARENT
        );
        mLayoutParams.gravity = Gravity.CENTER;

        initUnlockViews();
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_GO_HOME) {
                closeUnLockView();
            }
        }
    };

    /**
     * Entsperrungsansicht öffnen
     */
    public void showUnLockView(final String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(mContext)) {
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mPackageName = packageName;
                mFailedPatternAttemptsSinceLastTimeout = 0;
                mFailedPicAttempts = 0;
                mPatternView.clearPattern();
                mEtPinUnlock.setText("");
                initBgView();
                // Entsperrungsmethode bestimmen und passende UI anzeigen
                if (PinUtils.isPinMethod()) {
                    mPatternView.setVisibility(View.GONE);
                    mPinUnlockSection.setVisibility(View.VISIBLE);
                } else {
                    mPatternView.setVisibility(View.VISIBLE);
                    mPinUnlockSection.setVisibility(View.GONE);
                }
                if (!isShowing()) {
                    try {
                        mWindowManager.addView(UnlockView.this, mLayoutParams);
                    } catch (WindowManager.BadTokenException e) {
                        e.printStackTrace();
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * Entsperrungsansicht schließen
     */
    public boolean closeUnLockView() {
        if (mWindowManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (isAttachedToWindow()) {
                    mWindowManager.removeViewImmediate(this);
                    return true;
                } else {
                    return false;
                }
            } else {
                try {
                    if (getParent() != null) {
                        mWindowManager.removeViewImmediate(this);
                    }
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    /**
     * Prüfen ob das Fenster aktuell angezeigt wird
     */
    public boolean isShowing() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return isAttachedToWindow();
        } else {
            return getParent() != null;
        }
    }

    /**
     * Home-Taste: Ansicht verzögert schließen
     */
    public void closeUnLockViewFormHomeAction() {
        if (isShowing() && mHandler != null) {
            mHandler.sendEmptyMessageDelayed(MSG_GO_HOME, 500);
        }
    }

    /**
     * Hintergrundbild mit Weichzeichner-Effekt initialisieren
     */
    private void initBgView() {
        try {
            ApplicationInfo appInfo = mPackageManager.getApplicationInfo(
                    mPackageName, PackageManager.GET_UNINSTALLED_PACKAGES);
            if (appInfo != null) {
                final Drawable iconDrawable = mPackageManager.getApplicationIcon(appInfo);
                String appLabel = mPackageManager.getApplicationLabel(appInfo).toString();
                mUnLockIcon.setImageDrawable(iconDrawable);
                mAppLogo.setImageDrawable(iconDrawable);
                mUnLockText.setText(appLabel);
                mAppLabel.setText(appLabel);
                mUnlockFailTip.setText(PinUtils.isPinMethod()
                        ? mContext.getString(R.string.pin_enter_to_unlock)
                        : mContext.getString(R.string.password_gestrue_tips));
                mUnLockLayout.setBackgroundDrawable(iconDrawable);
                mUnLockLayout.getViewTreeObserver().addOnPreDrawListener(
                        new ViewTreeObserver.OnPreDrawListener() {
                            @Override
                            public boolean onPreDraw() {
                                mUnLockLayout.getViewTreeObserver().removeOnPreDrawListener(this);
                                mUnLockLayout.buildDrawingCache();
                                Bitmap bmp = LockUtil.drawableToBitmap(iconDrawable, mUnLockLayout);
                                LockUtil.blur(mContext, LockUtil.big(bmp), mUnLockLayout);
                                return true;
                            }
                        });
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Entsperr-Views initialisieren (sowohl PIN als auch Muster)
     */
    private void initUnlockViews() {
        initLockPatternView();
        initPinUnlockView();
    }

    /**
     * Muster-Entsperr-Widget initialisieren
     */
    private void initLockPatternView() {
        mPatternView.setLineColorRight(0x80ffffff);
        mPatternUtils = new LockPatternUtils(mContext);
        mPatternViewPattern = new LockPatternViewPattern(mPatternView);
        mPatternViewPattern.setPatternListener(new LockPatternViewPattern.onPatternListener() {
            @Override
            public void onPatternDetected(List<LockPatternView.Cell> pattern) {
                if (mPatternUtils.checkPattern(pattern)) {
                    mPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);
                    handleUnlockSuccess();
                } else {
                    mPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    takePhotoIfEnabled();
                    if (pattern.size() >= LockPatternUtils.MIN_PATTERN_REGISTER_FAIL) {
                        mFailedPatternAttemptsSinceLastTimeout++;
                        int retry = LockPatternUtils.FAILED_ATTEMPTS_BEFORE_TIMEOUT
                                - mFailedPatternAttemptsSinceLastTimeout;
                        if (retry >= 0) {
                            mUnlockFailTip.setText(
                                    mContext.getResources().getString(R.string.password_error_count));
                        }
                    } else {
                        mUnlockFailTip.setText(
                                mContext.getResources().getString(R.string.password_short));
                    }
                    mPatternView.postDelayed(mClearPatternRunnable, 500);
                }
            }
        });
        mPatternView.setOnPatternListener(mPatternViewPattern);
        mPatternView.setTactileFeedbackEnabled(true);
    }

    /**
     * PIN-Entsperrung initialisieren
     */
    private void initPinUnlockView() {
        mBtnPinUnlock.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String pin = mEtPinUnlock.getText().toString().trim();
                if (pin.length() < PinUtils.MIN_PIN_LENGTH) {
                    mUnlockFailTip.setText(mContext.getString(R.string.pin_too_short));
                    return;
                }
                if (PinUtils.checkPin(pin)) {
                    mEtPinUnlock.setText("");
                    handleUnlockSuccess();
                } else {
                    mEtPinUnlock.setText("");
                    mUnlockFailTip.setText(mContext.getString(R.string.pin_wrong));
                    takePhotoIfEnabled();
                }
            }
        });
    }

    /** Entsperrung erfolgreich – Zustand aktualisieren und Ansicht schließen */
    private void handleUnlockSuccess() {
        long unlockTime = System.currentTimeMillis();
        SpUtil.getInstance().putLong(AppConstants.LOCK_CURR_MILLISENCONS, unlockTime);
        SpUtil.getInstance().putString(AppConstants.LOCK_LAST_LOAD_PKG_NAME, mPackageName);
        LockService.sLockCurrMilliseconds = unlockTime;
        LockService.sLastLoadPkgName = mPackageName;

        Intent intent = new Intent(LockService.UNLOCK_ACTION);
        intent.putExtra(LockService.LOCK_SERVICE_LASTTIME, unlockTime);
        intent.putExtra(LockService.LOCK_SERVICE_LASTAPP, mPackageName);
        mContext.sendBroadcast(intent);

        mLockInfoManager.unlockCommApplication(mPackageName);
        closeUnLockView();
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
                new Camera2Manager(mContext).capturePhoto();
            }
        }
    }

    private final Runnable mClearPatternRunnable = new Runnable() {
        public void run() {
            mPatternView.clearPattern();
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter(FINISH_UNLOCK_THIS_APP);
        mContext.registerReceiver(mFinishReceiver, filter);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        try {
            mContext.unregisterReceiver(mFinishReceiver);
        } catch (IllegalArgumentException e) {
            // Empfänger war nicht registriert
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (isShowing()) {
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(homeIntent);
                mHandler.sendEmptyMessageDelayed(MSG_GO_HOME, 500);
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}

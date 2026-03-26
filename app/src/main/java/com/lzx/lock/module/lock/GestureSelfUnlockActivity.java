package com.lzx.lock.module.lock;

import android.content.Intent;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lzx.lock.R;
import com.lzx.lock.base.AppConstants;
import com.lzx.lock.base.BaseActivity;
import com.lzx.lock.db.CommLockInfoManager;
import com.lzx.lock.module.main.MainActivity;
import com.lzx.lock.module.setting.LockSettingActivity;
import com.lzx.lock.utils.LockPatternUtils;
import com.lzx.lock.utils.PinUtils;
import com.lzx.lock.utils.SpUtil;
import com.lzx.lock.utils.SystemBarHelper;
import com.lzx.lock.utils.ToastUtil;
import com.lzx.lock.widget.LockPatternView;
import com.lzx.lock.widget.LockPatternViewPattern;

import java.util.List;

/**
 * Entsperrungsbildschirm für die App selbst (beim App-Start)
 */
public class GestureSelfUnlockActivity extends BaseActivity implements View.OnClickListener {

    // PIN-Entsperrung
    private LinearLayout mPinUnlockSection;
    private EditText mEtPinUnlock;
    private TextView mBtnPinUnlock;

    // Muster-Entsperrung
    private LockPatternView mLockPatternView;
    private LockPatternUtils mLockPatternUtils;
    private LockPatternViewPattern mPatternViewPattern;
    private int mFailedPatternAttemptsSinceLastTimeout = 0;

    private String actionFrom;
    private String pkgName;
    private CommLockInfoManager mManager;
    private RelativeLayout mTopLayout;
    private TextureView mTextureView;

    @Override
    public int getLayoutId() {
        return R.layout.activity_gesture_self_unlock;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        mTopLayout = (RelativeLayout) findViewById(R.id.top_layout);
        mTextureView = (TextureView) findViewById(R.id.texture_view);
        mTopLayout.setPadding(0, SystemBarHelper.getStatusBarHeight(this), 0, 0);

        mPinUnlockSection = (LinearLayout) findViewById(R.id.pin_unlock_section);
        mEtPinUnlock = (EditText) findViewById(R.id.et_pin_unlock);
        mBtnPinUnlock = (TextView) findViewById(R.id.btn_pin_unlock);

        mLockPatternView = (LockPatternView) findViewById(R.id.unlock_lock_view);
    }

    @Override
    protected void initData() {
        mManager = new CommLockInfoManager(this);
        pkgName = getIntent().getStringExtra(AppConstants.LOCK_PACKAGE_NAME);
        actionFrom = getIntent().getStringExtra(AppConstants.LOCK_FROM);

        // Entsperrungsmethode bestimmen und passende UI anzeigen
        if (PinUtils.isPinMethod()) {
            showPinUnlock();
        } else {
            showPatternUnlock();
        }
    }

    /** PIN-Entsperrung anzeigen */
    private void showPinUnlock() {
        mPinUnlockSection.setVisibility(View.VISIBLE);
        mLockPatternView.setVisibility(View.GONE);
    }

    /** Muster-Entsperrung anzeigen */
    private void showPatternUnlock() {
        mPinUnlockSection.setVisibility(View.GONE);
        mLockPatternView.setVisibility(View.VISIBLE);
        initLockPatternView();
    }

    /**
     * Muster-Entsperr-Widget initialisieren
     */
    private void initLockPatternView() {
        mLockPatternUtils = new LockPatternUtils(this);
        mPatternViewPattern = new LockPatternViewPattern(mLockPatternView);
        mPatternViewPattern.setPatternListener(new LockPatternViewPattern.onPatternListener() {
            @Override
            public void onPatternDetected(List<LockPatternView.Cell> pattern) {
                if (mLockPatternUtils.checkPattern(pattern)) {
                    mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);
                    handleUnlockSuccess();
                } else {
                    mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    if (pattern.size() >= LockPatternUtils.MIN_PATTERN_REGISTER_FAIL) {
                        mFailedPatternAttemptsSinceLastTimeout++;
                    }
                    if (mFailedPatternAttemptsSinceLastTimeout >= LockPatternUtils.FAILED_ATTEMPTS_BEFORE_TIMEOUT) {
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
    protected void initAction() {
        mBtnPinUnlock.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_pin_unlock) {
            String pin = mEtPinUnlock.getText().toString();
            if (pin.length() < PinUtils.MIN_PIN_LENGTH) {
                ToastUtil.showToast(getString(R.string.pin_too_short));
                return;
            }
            if (PinUtils.checkPin(pin)) {
                mEtPinUnlock.setText("");
                handleUnlockSuccess();
            } else {
                mEtPinUnlock.setText("");
                ToastUtil.showToast(getString(R.string.pin_wrong));
            }
        }
    }

    /** Entsperrung erfolgreich – je nach Herkunft weiterleiten */
    private void handleUnlockSuccess() {
        if (AppConstants.LOCK_FROM_FINISH.equals(actionFrom)) {
            mManager.unlockCommApplication(pkgName);
            finish();
        } else if (AppConstants.LOCK_FROM_SETTING.equals(actionFrom)) {
            startActivity(new Intent(GestureSelfUnlockActivity.this, LockSettingActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        } else if (AppConstants.LOCK_FROM_UNLOCK.equals(actionFrom)) {
            mManager.setIsUnLockThisApp(pkgName, true);
            mManager.unlockCommApplication(pkgName);
            sendBroadcast(new Intent(UnlockActivity.FINISH_UNLOCK_THIS_APP));
            finish();
        } else {
            // LOCK_FROM_LOCK_MAIN_ACITVITY oder anderer Wert: zur Hauptansicht navigieren
            Intent intent = new Intent(GestureSelfUnlockActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }
    }
}

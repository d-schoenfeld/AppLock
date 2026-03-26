package com.lzx.lock.module.pwd;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lzx.lock.R;
import com.lzx.lock.base.AppConstants;
import com.lzx.lock.base.BaseActivity;
import com.lzx.lock.bean.LockStage;
import com.lzx.lock.module.main.MainActivity;
import com.lzx.lock.mvp.contract.GestureCreateContract;
import com.lzx.lock.mvp.p.GestureCreatePresenter;
import com.lzx.lock.service.LockService;
import com.lzx.lock.utils.LockPatternUtils;
import com.lzx.lock.utils.PinUtils;
import com.lzx.lock.utils.SpUtil;
import com.lzx.lock.utils.SystemBarHelper;
import com.lzx.lock.utils.ToastUtil;
import com.lzx.lock.widget.LockPatternView;
import com.lzx.lock.widget.LockPatternViewPattern;

import java.util.List;

/**
 * Erstes Einrichten: Sperrmethode und Passwort/Muster festlegen
 */
public class CreatePwdActivity extends BaseActivity implements View.OnClickListener,
        GestureCreateContract.View {

    // Methoden-Auswahl
    private TextView mBtnSelectPin;
    private TextView mBtnSelectPattern;
    private LinearLayout mPinSection;

    // PIN-Abschnitt
    private TextView mPinTip;
    private EditText mEtPin;
    private EditText mEtPinConfirm;
    private TextView mBtnPinNext;

    // Muster-Abschnitt
    private TextView mLockTip;
    private LockPatternView mLockPatternView;
    private TextView mBtnReset;

    // Mustersperre-bezogen
    private LockStage mUiStage = LockStage.Introduction;
    protected List<LockPatternView.Cell> mChosenPattern = null;
    private LockPatternUtils mLockPatternUtils;
    private LockPatternViewPattern mPatternViewPattern;
    private GestureCreatePresenter mGestureCreatePresenter;
    private RelativeLayout mTopLayout;

    @Override
    public int getLayoutId() {
        return R.layout.activity_create_pwd;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        mTopLayout = (RelativeLayout) findViewById(R.id.top_layout);
        mTopLayout.setPadding(0, SystemBarHelper.getStatusBarHeight(this), 0, 0);

        mBtnSelectPin = (TextView) findViewById(R.id.btn_select_pin);
        mBtnSelectPattern = (TextView) findViewById(R.id.btn_select_pattern);
        mPinSection = (LinearLayout) findViewById(R.id.pin_section);
        mPinTip = (TextView) findViewById(R.id.pin_tip);
        mEtPin = (EditText) findViewById(R.id.et_pin);
        mEtPinConfirm = (EditText) findViewById(R.id.et_pin_confirm);
        mBtnPinNext = (TextView) findViewById(R.id.btn_pin_next);

        mLockTip = (TextView) findViewById(R.id.lock_tip);
        mLockPatternView = (LockPatternView) findViewById(R.id.lock_pattern_view);
        mBtnReset = (TextView) findViewById(R.id.btn_reset);
    }

    @Override
    protected void initData() {
        mGestureCreatePresenter = new GestureCreatePresenter(this, this);
        initLockPatternView();
        // Standard: PIN-Modus
        showPinMode();
    }

    /**
     * Entsperr-Widget initialisieren
     */
    private void initLockPatternView() {
        mLockPatternUtils = new LockPatternUtils(this);
        mPatternViewPattern = new LockPatternViewPattern(mLockPatternView);
        mPatternViewPattern.setPatternListener(new LockPatternViewPattern.onPatternListener() {
            @Override
            public void onPatternDetected(List<LockPatternView.Cell> pattern) {
                mGestureCreatePresenter.onPatternDetected(pattern, mChosenPattern, mUiStage);
            }
        });
        mLockPatternView.setOnPatternListener(mPatternViewPattern);
        mLockPatternView.setTactileFeedbackEnabled(true);
    }

    @Override
    protected void initAction() {
        mBtnSelectPin.setOnClickListener(this);
        mBtnSelectPattern.setOnClickListener(this);
        mBtnPinNext.setOnClickListener(this);
        mBtnReset.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_select_pin:
                showPinMode();
                break;
            case R.id.btn_select_pattern:
                showPatternMode();
                break;
            case R.id.btn_pin_next:
                handlePinNextStep();
                break;
            case R.id.btn_reset:
                setStepOne();
                break;
        }
    }

    /** Wechsel zu PIN-Modus */
    private void showPinMode() {
        mEtPin.setText("");
        mEtPinConfirm.setText("");
        mPinTip.setText(getString(R.string.pin_enter_hint));
        mLockTip.setText(getString(R.string.pin_set_header));
        mPinSection.setVisibility(View.VISIBLE);
        mLockPatternView.setVisibility(View.GONE);
        mBtnReset.setVisibility(View.GONE);

        // Schaltflächen-Hervorhebung aktualisieren
        mBtnSelectPin.setBackgroundResource(R.drawable.bg_btn_blue);
        mBtnSelectPin.setTextColor(getResources().getColor(R.color.white));
        mBtnSelectPattern.setBackgroundResource(R.drawable.bg_btn_method_unselected);
        mBtnSelectPattern.setTextColor(getResources().getColor(R.color.font_deep_gray));
    }

    /** Wechsel zu Muster-Modus */
    private void showPatternMode() {
        mPinSection.setVisibility(View.GONE);
        mLockPatternView.setVisibility(View.VISIBLE);
        mBtnReset.setVisibility(View.VISIBLE);
        mGestureCreatePresenter.updateStage(LockStage.Introduction);

        // Schaltflächen-Hervorhebung aktualisieren
        mBtnSelectPattern.setBackgroundResource(R.drawable.bg_btn_blue);
        mBtnSelectPattern.setTextColor(getResources().getColor(R.color.white));
        mBtnSelectPin.setBackgroundResource(R.drawable.bg_btn_method_unselected);
        mBtnSelectPin.setTextColor(getResources().getColor(R.color.font_deep_gray));
    }

    /** PIN-Eingabe bestätigen und speichern */
    private void handlePinNextStep() {
        String pin = mEtPin.getText().toString();
        String confirmPin = mEtPinConfirm.getText().toString();
        if (pin.length() < PinUtils.MIN_PIN_LENGTH) {
            ToastUtil.showToast(getString(R.string.pin_too_short));
            return;
        }
        if (!pin.equals(confirmPin)) {
            ToastUtil.showToast(getString(R.string.pin_mismatch));
            mEtPin.setText("");
            mEtPinConfirm.setText("");
            return;
        }
        // PIN gespeichern
        PinUtils.savePin(pin);
        SpUtil.getInstance().putString(AppConstants.LOCK_METHOD, AppConstants.LOCK_METHOD_PIN);
        gotoLockMainActivity();
    }

    /**
     * Zum ersten Schritt zurückgehen (Muster-Modus)
     */
    private void setStepOne() {
        mGestureCreatePresenter.updateStage(LockStage.Introduction);
        mLockTip.setText(getString(R.string.lock_recording_intro_header));
    }

    private void gotoLockMainActivity() {
        SpUtil.getInstance().putBoolean(AppConstants.LOCK_STATE, true);
        Intent serviceIntent = new Intent(this, LockService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        SpUtil.getInstance().putBoolean(AppConstants.LOCK_IS_FIRST_LOCK, false);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    // --- GestureCreateContract.View ---

    @Override
    public void updateUiStage(LockStage stage) {
        mUiStage = stage;
    }

    @Override
    public void updateChosenPattern(List<LockPatternView.Cell> mChosenPattern) {
        this.mChosenPattern = mChosenPattern;
    }

    @Override
    public void updateLockTip(String text, boolean isToast) {
        mLockTip.setText(text);
    }

    @Override
    public void setHeaderMessage(int headerMessage) {
        mLockTip.setText(headerMessage);
    }

    @Override
    public void lockPatternViewConfiguration(boolean patternEnabled, LockPatternView.DisplayMode displayMode) {
        if (patternEnabled) {
            mLockPatternView.enableInput();
        } else {
            mLockPatternView.disableInput();
        }
        mLockPatternView.setDisplayMode(displayMode);
    }

    @Override
    public void Introduction() {
        clearPattern();
    }

    @Override
    public void HelpScreen() {
    }

    @Override
    public void ChoiceTooShort() {
        mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
        mLockPatternView.removeCallbacks(mClearPatternRunnable);
        mLockPatternView.postDelayed(mClearPatternRunnable, 500);
    }

    private Runnable mClearPatternRunnable = new Runnable() {
        public void run() {
            mLockPatternView.clearPattern();
        }
    };

    @Override
    public void moveToStatusTwo() {
    }

    @Override
    public void clearPattern() {
        mLockPatternView.clearPattern();
    }

    @Override
    public void ConfirmWrong() {
        mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
        mLockPatternView.removeCallbacks(mClearPatternRunnable);
        mLockPatternView.postDelayed(mClearPatternRunnable, 500);
    }

    @Override
    public void ChoiceConfirmed() {
        mLockPatternUtils.saveLockPattern(mChosenPattern);
        SpUtil.getInstance().putString(AppConstants.LOCK_METHOD, AppConstants.LOCK_METHOD_PATTERN);
        clearPattern();
        gotoLockMainActivity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGestureCreatePresenter.onDestroy();
    }
}

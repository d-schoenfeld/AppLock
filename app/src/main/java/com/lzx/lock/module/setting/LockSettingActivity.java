package com.lzx.lock.module.setting;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lzx.lock.R;
import com.lzx.lock.module.lock.GestureCreateActivity;
import com.lzx.lock.base.BaseActivity;
import com.lzx.lock.base.AppConstants;
import com.lzx.lock.bean.LockAutoTime;
import com.lzx.lock.service.LockService;
import com.lzx.lock.utils.SpUtil;
import com.lzx.lock.utils.SystemBarHelper;
import com.lzx.lock.utils.ToastUtil;
import com.lzx.lock.widget.SelectLockTimeDialog;


/**
 * Created by xian on 2017/2/17.
 */

public class LockSettingActivity extends BaseActivity implements View.OnClickListener
        , DialogInterface.OnDismissListener {
    private TextView mLockTime, mBtnChangePwd, mLockTip, mLockScreenSwitch,mLockTakePicSwitch, mLockTakePicAttemptValue;
    private CheckBox mLockSwitch;
    private RelativeLayout mLockWhen, mLockScreen,mLockTakePic, mLockTakePicAttempts;
    private LockSettingReceiver mLockSettingReceiver;
    public static final String ON_ITEM_CLICK_ACTION = "on_item_click_action";
    private SelectLockTimeDialog dialog;
    private static final int REQUEST_CHANGE_PWD = 3;
    private static final int PERMISSION_REQUEST_CAMERA = 100;
    private RelativeLayout mTopLayout;

    @Override
    public int getLayoutId() {
        return R.layout.activity_setting;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        mBtnChangePwd = (TextView) findViewById(R.id.btn_change_pwd);
        mLockTime = (TextView) findViewById(R.id.lock_time);
        mLockSwitch = (CheckBox) findViewById(R.id.switch_compat);
        mLockWhen = (RelativeLayout) findViewById(R.id.lock_when);
        mLockScreen = (RelativeLayout) findViewById(R.id.lock_screen);
        mLockTakePic = (RelativeLayout) findViewById(R.id.lock_take_pic);
        mLockTakePicAttempts = (RelativeLayout) findViewById(R.id.lock_take_pic_attempts);
        mLockTip = (TextView) findViewById(R.id.lock_tip);
        mLockScreenSwitch = (TextView) findViewById(R.id.lock_screen_switch);
        mLockTakePicSwitch = (TextView) findViewById(R.id.lock_take_pic_switch);
        mLockTakePicAttemptValue = (TextView) findViewById(R.id.lock_take_pic_attempt_value);
        mTopLayout = (RelativeLayout) findViewById(R.id.top_layout);
        mTopLayout.setPadding(0, SystemBarHelper.getStatusBarHeight(this), 0, 0);
    }

    @Override
    protected void initData() {
        mLockSettingReceiver = new LockSettingReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ON_ITEM_CLICK_ACTION);
        registerReceiver(mLockSettingReceiver, filter);
        dialog = new SelectLockTimeDialog(this, "");
        dialog.setOnDismissListener(this);
        boolean isLockOpen = SpUtil.getInstance().getBoolean(AppConstants.LOCK_STATE);
        mLockSwitch.setChecked(isLockOpen);

        boolean isLockAutoScreen = SpUtil.getInstance().getBoolean(AppConstants.LOCK_AUTO_SCREEN, false);
        mLockScreenSwitch.setText(isLockAutoScreen ? "Ein" : "Aus");

        boolean isTakePic = SpUtil.getInstance().getBoolean(AppConstants.LOCK_AUTO_RECORD_PIC,false);
        mLockTakePicSwitch.setText(isTakePic ? "Ein" : "Aus");
        if (isTakePic) {
            mLockTakePicAttempts.setVisibility(View.VISIBLE);
        }
        int attempt = (int) SpUtil.getInstance().getInt(AppConstants.LOCK_AUTO_RECORD_PIC_ATTEMPT, 1);
        mLockTakePicAttemptValue.setText(String.valueOf(attempt));

        mLockTime.setText(SpUtil.getInstance().getString(AppConstants.LOCK_APART_TITLE,"Sofort"));
    }

    @Override
    protected void initAction() {
        mBtnChangePwd.setOnClickListener(this);
        mLockWhen.setOnClickListener(this);
        mLockScreen.setOnClickListener(this);
        mLockScreenSwitch.setOnClickListener(this);
        mLockTakePic.setOnClickListener(this);
        mLockTakePicAttempts.setOnClickListener(this);
        mLockSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SpUtil.getInstance().putBoolean(AppConstants.LOCK_STATE, b);
                Intent intent = new Intent(LockSettingActivity.this, LockService.class);
                if (b) {
                    mLockTip.setText("Aktiviert – gesperrte Apps erfordern ein Passwort");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent);
                    } else {
                        startService(intent);
                    }
                } else {
                    mLockTip.setText("Deaktiviert – gesperrte Apps erfordern kein Passwort");
                    stopService(intent);
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_change_pwd:
                Intent intent = new Intent(LockSettingActivity.this, GestureCreateActivity.class);
                startActivityForResult(intent, REQUEST_CHANGE_PWD);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                break;
            case R.id.lock_when:
                String title = SpUtil.getInstance().getString(AppConstants.LOCK_APART_TITLE, "");
                dialog.setTitle(title);
                dialog.show();
                break;
            case R.id.lock_screen:
                boolean isLockAutoScreen = SpUtil.getInstance().getBoolean(AppConstants.LOCK_AUTO_SCREEN, false);
                if (isLockAutoScreen) {
                    SpUtil.getInstance().putBoolean(AppConstants.LOCK_AUTO_SCREEN, false);
                    LockService.sLockAutoScreen = false; //Statische Variable aktualisieren
                    mLockScreenSwitch.setText("Aus");
                } else {
                    SpUtil.getInstance().putBoolean(AppConstants.LOCK_AUTO_SCREEN, true);
                    LockService.sLockAutoScreen = true; //Statische Variable aktualisieren
                    mLockScreenSwitch.setText("Ein");
                }
                break;
            case R.id.lock_take_pic:
                boolean isTakePic = SpUtil.getInstance().getBoolean(AppConstants.LOCK_AUTO_RECORD_PIC,false);
                if (isTakePic) {
                    SpUtil.getInstance().putBoolean(AppConstants.LOCK_AUTO_RECORD_PIC, false);
                    mLockTakePicSwitch.setText("Aus");
                    mLockTakePicAttempts.setVisibility(View.GONE);
                } else {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED) {
                        enableTakePicOption();
                    } else {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.CAMERA},
                                PERMISSION_REQUEST_CAMERA);
                    }
                }
                break;
            case R.id.lock_take_pic_attempts:
                showAttemptPickerDialog();
                break;
        }
    }

    private void enableTakePicOption() {
        SpUtil.getInstance().putBoolean(AppConstants.LOCK_AUTO_RECORD_PIC, true);
        mLockTakePicSwitch.setText("Ein");
        mLockTakePicAttempts.setVisibility(View.VISIBLE);
    }

    private void showAttemptPickerDialog() {
        final int[] values = getResources().getIntArray(R.array.take_pic_attempt_values);
        final String[] labels = getResources().getStringArray(R.array.take_pic_attempt_labels);
        int currentAttempt = (int) SpUtil.getInstance().getInt(AppConstants.LOCK_AUTO_RECORD_PIC_ATTEMPT, 1);
        int checkedItem = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == currentAttempt) {
                checkedItem = i;
                break;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.take_pic_attempt_dialog_title)
                .setSingleChoiceItems(labels, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int selected = values[which];
                        SpUtil.getInstance().putInt(AppConstants.LOCK_AUTO_RECORD_PIC_ATTEMPT, selected);
                        mLockTakePicAttemptValue.setText(String.valueOf(selected));
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableTakePicOption();
            } else {
                mLockTakePicSwitch.setText("Aus");
                ToastUtil.showToast("Kamera-Berechtigung erforderlich – Option nicht aktiviert. Berechtigung in den App-Einstellungen erteilen.");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CHANGE_PWD:
                    ToastUtil.showToast("Passwort erfolgreich zurückgesetzt");
                    break;
            }
        }
    }


    @Override
    public void onDismiss(DialogInterface dialogInterface) {

    }

    private class LockSettingReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ON_ITEM_CLICK_ACTION)) {
                LockAutoTime info = intent.getParcelableExtra("info");
                boolean isLast = intent.getBooleanExtra("isLast", true);
                if (isLast) {
                    mLockTime.setText(info.getTitle());
                    SpUtil.getInstance().putString(AppConstants.LOCK_APART_TITLE, info.getTitle());
                    SpUtil.getInstance().putLong(AppConstants.LOCK_APART_MILLISENCONS, 0L);
                    SpUtil.getInstance().putBoolean(AppConstants.LOCK_AUTO_SCREEN_TIME, false);
                    LockService.sLockApartMilliseconds = 0L; //Statische Variable aktualisieren
                    LockService.sLockAutoScreenTime = false; //Statische Variable aktualisieren
                } else {
                    mLockTime.setText(info.getTitle());
                    SpUtil.getInstance().putString(AppConstants.LOCK_APART_TITLE, info.getTitle());
                    SpUtil.getInstance().putLong(AppConstants.LOCK_APART_MILLISENCONS, info.getTime());
                    SpUtil.getInstance().putBoolean(AppConstants.LOCK_AUTO_SCREEN_TIME, true);
                    LockService.sLockApartMilliseconds = info.getTime(); //Statische Variable aktualisieren
                    LockService.sLockAutoScreenTime = true; //Statische Variable aktualisieren
                }
                dialog.dismiss();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mLockSettingReceiver);
    }

}

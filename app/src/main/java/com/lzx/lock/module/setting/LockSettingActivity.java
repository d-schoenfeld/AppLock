package com.lzx.lock.module.setting;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lzx.lock.R;
import com.lzx.lock.module.about.AboutMeActivity;
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
    private TextView mBtnAbout, mLockTime, mBtnChangePwd, mIsShowPath, mLockTip, mLockScreenSwitch,mLockTakePicSwitch;
    private CheckBox mLockSwitch;
    private RelativeLayout mLockWhen, mLockScreen,mLockTakePic;
    private LockSettingReceiver mLockSettingReceiver;
    public static final String ON_ITEM_CLICK_ACTION = "on_item_click_action";
    private SelectLockTimeDialog dialog;
    private static final int REQUEST_CHANGE_PWD = 3;
    private RelativeLayout mTopLayout;

    @Override
    public int getLayoutId() {
        return R.layout.activity_setting;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        mBtnChangePwd = (TextView) findViewById(R.id.btn_change_pwd);
        mLockTime = (TextView) findViewById(R.id.lock_time);
        mBtnAbout = (TextView) findViewById(R.id.about_me);
        mLockSwitch = (CheckBox) findViewById(R.id.switch_compat);
        mLockWhen = (RelativeLayout) findViewById(R.id.lock_when);
        mLockScreen = (RelativeLayout) findViewById(R.id.lock_screen);
        mLockTakePic = (RelativeLayout) findViewById(R.id.lock_take_pic);
        mIsShowPath = (TextView) findViewById(R.id.is_show_path);
        mLockTip = (TextView) findViewById(R.id.lock_tip);
        mLockScreenSwitch = (TextView) findViewById(R.id.lock_screen_switch);
        mLockTakePicSwitch = (TextView) findViewById(R.id.lock_take_pic_switch);
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

        mLockTime.setText(SpUtil.getInstance().getString(AppConstants.LOCK_APART_TITLE,"Sofort"));
    }

    @Override
    protected void initAction() {
        mBtnChangePwd.setOnClickListener(this);
        mBtnAbout.setOnClickListener(this);
        mLockWhen.setOnClickListener(this);
        mLockScreen.setOnClickListener(this);
        mIsShowPath.setOnClickListener(this);
        mLockScreenSwitch.setOnClickListener(this);
        mLockTakePic.setOnClickListener(this);
        mLockSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SpUtil.getInstance().putBoolean(AppConstants.LOCK_STATE, b);
                Intent intent = new Intent(LockSettingActivity.this, LockService.class);
                if (b) {
                    mLockTip.setText("Aktiviert – gesperrte Apps erfordern ein Passwort");
                    startService(intent);
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
            case R.id.about_me:
                intent = new Intent(LockSettingActivity.this, AboutMeActivity.class);
                startActivity(intent);
                break;
            case R.id.lock_when:
                String title = SpUtil.getInstance().getString(AppConstants.LOCK_APART_TITLE, "");
                dialog.setTitle(title);
                dialog.show();
                break;
            case R.id.is_show_path:
                boolean ishideline = SpUtil.getInstance().getBoolean(AppConstants.LOCK_IS_HIDE_LINE, false);
                if (ishideline) {
                    SpUtil.getInstance().putBoolean(AppConstants.LOCK_IS_HIDE_LINE, false);
                    ToastUtil.showToast("Muster wird angezeigt");
                } else {
                    SpUtil.getInstance().putBoolean(AppConstants.LOCK_IS_HIDE_LINE, true);
                    ToastUtil.showToast("Muster wird ausgeblendet");
                }
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
                } else {
                    SpUtil.getInstance().putBoolean(AppConstants.LOCK_AUTO_RECORD_PIC, true);
                    mLockTakePicSwitch.setText("Ein");
                }
                break;
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

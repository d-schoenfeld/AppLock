package com.lzx.lock.module.splash;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import com.lzx.lock.R;
import com.lzx.lock.base.BaseActivity;
import com.lzx.lock.utils.LockUtil;
import com.lzx.lock.utils.ToastUtil;

/**
 * Einrichtungsassistent für App-Berechtigungen.
 *
 * Führt den Nutzer Schritt für Schritt durch die benötigten Berechtigungen:
 *  - App-Nutzungsdaten (erforderlich): Erkennung der aktiven App
 *  - Über anderen Apps einblenden (erforderlich): Anzeige des Sperrbildschirms
 *  - Genaue Alarme (optional, ab Android 12): Zuverlässiger Dienst-Neustart
 *  - Kamera (optional): Automatisches Foto bei falschem Passwort
 *
 * Bereits erteilte Berechtigungen werden automatisch übersprungen, mit Ausnahme der
 * Alarm-Berechtigung: Diese wird stets angezeigt, da sie auf Android 12 (API 31–32) für
 * Apps mit targetSdkVersion ≤ 32 automatisch gewährt wird und daher nicht zuverlässig
 * als „bereits erteilt" erkennbar ist.
 * Ohne die erforderlichen Berechtigungen wird der App-Start abgebrochen.
 */
public class PermissionSetupActivity extends BaseActivity {

    private static final int RC_USAGE_STATS = 10;
    private static final int RC_OVERLAY = 11;
    private static final int RC_EXACT_ALARM = 12;
    private static final int RC_CAMERA = 13;

    private static final int STEP_USAGE_STATS = 0;
    private static final int STEP_OVERLAY = 1;
    private static final int STEP_EXACT_ALARM = 2;
    private static final int STEP_CAMERA = 3;
    private static final int STEP_COUNT = 4;

    private int mCurrentStep = STEP_USAGE_STATS;
    private int mTotalDisplaySteps;
    private int mCurrentDisplayStep;

    private TextView mTvStepIndicator;
    private TextView mTvTitle;
    private TextView mTvBadge;
    private TextView mTvDescription;
    private TextView mBtnGrant;
    private TextView mBtnSkip;

    @Override
    public int getLayoutId() {
        return R.layout.activity_permission_setup;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        mTvStepIndicator = (TextView) findViewById(R.id.tv_step_indicator);
        mTvTitle = (TextView) findViewById(R.id.tv_permission_title);
        mTvBadge = (TextView) findViewById(R.id.tv_badge);
        mTvDescription = (TextView) findViewById(R.id.tv_permission_description);
        mBtnGrant = (TextView) findViewById(R.id.btn_grant);
        mBtnSkip = (TextView) findViewById(R.id.btn_skip);
    }

    @Override
    protected void initData() {
        mTotalDisplaySteps = countPendingSteps();
        mCurrentDisplayStep = 0;
        processCurrentStep();
    }

    @Override
    protected void initAction() {
        mBtnGrant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestCurrentPermission();
            }
        });
        mBtnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                advanceToNextStep();
            }
        });
    }

    /**
     * Zählt die Schritte, die dem Nutzer noch angezeigt werden müssen.
     */
    private int countPendingSteps() {
        int count = 0;
        for (int step = 0; step < STEP_COUNT; step++) {
            if (!isStepGranted(step) && isStepApplicable(step)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Prüft ob der angegebene Schritt auf diesem Gerät relevant ist.
     */
    private boolean isStepApplicable(int step) {
        if (step == STEP_EXACT_ALARM) {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
        }
        return true;
    }

    /**
     * Prüft ob die Berechtigung für den angegebenen Schritt bereits erteilt ist.
     */
    private boolean isStepGranted(int step) {
        switch (step) {
            case STEP_USAGE_STATS:
                return LockUtil.isStatAccessPermissionSet(this);
            case STEP_OVERLAY:
                return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                        || Settings.canDrawOverlays(this);
            case STEP_EXACT_ALARM:
                // Always show the alarm step on supported Android versions (API 31+).
                // On Android 12 (API 31–32), the permission is auto-granted for apps
                // targeting API ≤ 32, so canScheduleExactAlarms() returns true even
                // without explicit user interaction.  Returning false here ensures the
                // step is always presented so the user is aware of this permission.
                return false;
            case STEP_CAMERA:
                return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED;
            default:
                return true;
        }
    }

    /**
     * Verarbeitet den aktuellen Schritt: überspringt bereits erteilte Berechtigungen,
     * zeigt den nächsten ausstehenden Schritt an oder schließt den Assistenten.
     */
    private void processCurrentStep() {
        // Bereits erteilte oder nicht anwendbare Schritte überspringen
        while (mCurrentStep < STEP_COUNT
                && (!isStepApplicable(mCurrentStep) || isStepGranted(mCurrentStep))) {
            mCurrentStep++;
        }
        if (mCurrentStep >= STEP_COUNT) {
            finishWithSuccess();
            return;
        }
        mCurrentDisplayStep++;
        updateUi();
    }

    /**
     * Aktualisiert die Benutzeroberfläche für den aktuellen Schritt.
     */
    private void updateUi() {
        mTvStepIndicator.setText(
                getString(R.string.perm_step_indicator, mCurrentDisplayStep, mTotalDisplaySteps));

        boolean required = isCurrentStepRequired();

        switch (mCurrentStep) {
            case STEP_USAGE_STATS:
                mTvTitle.setText(R.string.perm_usage_stats_title);
                mTvDescription.setText(R.string.perm_usage_stats_desc);
                break;
            case STEP_OVERLAY:
                mTvTitle.setText(R.string.perm_overlay_title);
                mTvDescription.setText(R.string.perm_overlay_desc);
                break;
            case STEP_EXACT_ALARM:
                mTvTitle.setText(R.string.perm_exact_alarm_title);
                mTvDescription.setText(R.string.perm_exact_alarm_desc);
                break;
            case STEP_CAMERA:
                mTvTitle.setText(R.string.perm_camera_title);
                mTvDescription.setText(R.string.perm_camera_desc);
                break;
        }

        if (required) {
            mTvBadge.setText(R.string.perm_required);
            mTvBadge.setBackgroundResource(R.drawable.bg_btn_blue);
            mTvBadge.setTextColor(Color.WHITE);
            mBtnSkip.setVisibility(View.GONE);
        } else {
            mTvBadge.setText(R.string.perm_optional);
            mTvBadge.setBackgroundResource(R.drawable.bg_btn_method_unselected);
            mTvBadge.setTextColor(ContextCompat.getColor(this, R.color.font_deep_gray));
            mBtnSkip.setVisibility(View.VISIBLE);
            mBtnSkip.setText(R.string.perm_skip_button);
        }

        mBtnGrant.setText(R.string.perm_grant_button);
    }

    /**
     * Gibt an ob der aktuelle Schritt eine erforderliche Berechtigung ist.
     */
    private boolean isCurrentStepRequired() {
        return mCurrentStep == STEP_USAGE_STATS || mCurrentStep == STEP_OVERLAY;
    }

    /**
     * Startet den Systemdialog zur Erteilung der aktuellen Berechtigung.
     */
    private void requestCurrentPermission() {
        switch (mCurrentStep) {
            case STEP_USAGE_STATS:
                // On Android 10+ (API 29+), a package URI can be supplied to go
                // directly to AppLock's usage-access settings page instead of
                // showing the list of all apps.
                Intent usageIntent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    usageIntent.setData(Uri.parse("package:" + getPackageName()));
                }
                startActivityForResult(usageIntent, RC_USAGE_STATS);
                break;
            case STEP_OVERLAY:
                startActivityForResult(
                        new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName())),
                        RC_OVERLAY);
                break;
            case STEP_EXACT_ALARM:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    startActivityForResult(
                            new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:" + getPackageName())),
                            RC_EXACT_ALARM);
                }
                break;
            case STEP_CAMERA:
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.CAMERA},
                        RC_CAMERA);
                break;
        }
    }

    /**
     * Rückkehr von einem Einstellungs-Bildschirm: prüft ob die Berechtigung erteilt wurde.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_USAGE_STATS) {
            if (isStepGranted(STEP_USAGE_STATS)) {
                advanceToNextStep();
            } else {
                ToastUtil.showToast(getString(R.string.perm_required_hint));
            }
        } else if (requestCode == RC_OVERLAY) {
            if (isStepGranted(STEP_OVERLAY)) {
                advanceToNextStep();
            } else {
                ToastUtil.showToast(getString(R.string.perm_required_hint));
            }
        } else if (requestCode == RC_EXACT_ALARM) {
            // Optional – Ergebnis ist unerheblich
            advanceToNextStep();
        }
    }

    /**
     * Rückkehr vom Kamera-Berechtigungsdialog.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_CAMERA) {
            // Optional – Ergebnis ist unerheblich
            advanceToNextStep();
        }
    }

    /**
     * Wechselt zum nächsten Schritt.
     */
    private void advanceToNextStep() {
        mCurrentStep++;
        processCurrentStep();
    }

    /**
     * Schließt den Assistenten erfolgreich (alle erforderlichen Berechtigungen erteilt).
     */
    private void finishWithSuccess() {
        setResult(RESULT_OK);
        finish();
    }

    /**
     * Rückwärts-Navigation: bei erforderlichen Berechtigungen App beenden,
     * bei optionalen Schritt überspringen.
     */
    @Override
    public void onBackPressed() {
        if (isCurrentStepRequired()) {
            ToastUtil.showToast(getString(R.string.perm_required_hint));
            setResult(RESULT_CANCELED);
            finish();
        } else {
            advanceToNextStep();
        }
    }
}

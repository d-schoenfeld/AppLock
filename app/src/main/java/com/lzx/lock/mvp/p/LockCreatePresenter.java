package com.lzx.lock.mvp.p;

import android.content.Context;

import com.lzx.lock.R;
import com.lzx.lock.bean.LockStage;
import com.lzx.lock.mvp.contract.LockCreateContract;
import com.lzx.lock.utils.LockPatternUtils;
import com.lzx.lock.widget.LockPatternView;

import java.util.ArrayList;
import java.util.List;

import static com.lzx.lock.bean.LockStage.ChoiceConfirmed;
import static com.lzx.lock.bean.LockStage.ChoiceTooShort;
import static com.lzx.lock.bean.LockStage.ConfirmWrong;
import static com.lzx.lock.bean.LockStage.FirstChoiceValid;
import static com.lzx.lock.bean.LockStage.Introduction;
import static com.lzx.lock.bean.LockStage.NeedToConfirm;

/**
 * Created by xian on 2017/2/17.
 */

public class LockCreatePresenter implements LockCreateContract.Presenter {
    private LockCreateContract.View mView;
    private Context mContext;

    public LockCreatePresenter(LockCreateContract.View view, Context context) {
        mView = view;
        mContext = context;
    }

    @Override
    public void updateStage(LockStage stage) {
        mView.updateUiStage(stage); //UiStage aktualisieren
        if (stage == ChoiceTooShort) { //wenn weniger als 4 Punkte
            mView.updateLockTip(mContext.getResources().getString(stage.headerMessage, LockPatternUtils.MIN_LOCK_PATTERN_SIZE), true);
        } else {
            if (stage.headerMessage == R.string.lock_need_to_unlock_wrong) {
                mView.updateLockTip(mContext.getResources().getString(R.string.lock_need_to_unlock_wrong), true);
                mView.setHeaderMessage(R.string.lock_recording_intro_header);
            } else {
                mView.setHeaderMessage(stage.headerMessage); //
            }
        }
        // same for whether the patten is enabled
        mView.lockPatternViewConfiguration(stage.patternEnabled, LockPatternView.DisplayMode.Correct);

        switch (stage) {
            case Introduction:  //Einführung
                mView.Introduction(); //Schritt 1
                break;
            case HelpScreen: //Hilfe (nach wie vielen Fehlern die Hilfsanimation startet)
                mView.HelpScreen();
                break;
            case ChoiceTooShort: //Entsperrmuster zu kurz
                mView.ChoiceTooShort();
                break;
            case FirstChoiceValid: //Schritt 1 erfolgreich abgeschlossen
                updateStage(NeedToConfirm); //weiter zu Schritt 2
                mView.moveToStatusTwo();
                break;
            case NeedToConfirm:
                mView.clearPattern();  //Schritt 2
                break;
            case ConfirmWrong:
                //Schritt 2 unterscheidet sich von Schritt 1
                mView.ConfirmWrong();
                break;
            case ChoiceConfirmed:
                //Schritt 3
                mView.ChoiceConfirmed();
                break;
        }
    }

    @Override
    public void onPatternDetected(List<LockPatternView.Cell> pattern, List<LockPatternView.Cell> mChosenPattern, LockStage mUiStage) {
        if (mUiStage == NeedToConfirm) { //wenn nächster Schritt
            if (mChosenPattern == null)
                throw new IllegalStateException("null chosen pattern in stage 'need to confirm");
            if (mChosenPattern.equals(pattern)) {
                updateStage(ChoiceConfirmed);
            } else {
                updateStage(ConfirmWrong);
            }
        } else if (mUiStage == ConfirmWrong) {
            if (pattern.size() < LockPatternUtils.MIN_LOCK_PATTERN_SIZE) {
                updateStage(ChoiceTooShort);
            } else {
                if (mChosenPattern.equals(pattern)) {
                    updateStage(ChoiceConfirmed);
                } else {
                    updateStage(ConfirmWrong);
                }
            }
        } else if (mUiStage == Introduction || mUiStage == ChoiceTooShort) {
            if (pattern.size() < LockPatternUtils.MIN_LOCK_PATTERN_SIZE) {
                updateStage(ChoiceTooShort);
            } else {
                mChosenPattern = new ArrayList<>(pattern);
                mView.updateChosenPattern(mChosenPattern);
                updateStage(FirstChoiceValid);
            }
        } else {
            throw new IllegalStateException("Unexpected stage " + mUiStage + " when " + "entering the pattern.");
        }
    }

    @Override
    public void onDestroy() {

    }
}
